/**
 * Copyright 2026 Naftiko
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.polychro.wellformedness;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.polychro.spi.Diagnostic;
import io.polychro.spi.Document;
import io.polychro.spi.Severity;
import io.polychro.spi.Validator;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.events.CollectionEndEvent;
import org.yaml.snakeyaml.events.Event;
import org.yaml.snakeyaml.events.MappingStartEvent;
import org.yaml.snakeyaml.events.NodeEvent;
import org.yaml.snakeyaml.events.ScalarEvent;
import org.yaml.snakeyaml.events.SequenceStartEvent;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validates document well-formedness: duplicate keys, encoding, YAML-specific checks, and depth/size limits.
 */
class WellformednessValidator implements Validator {

    static final String NAME = "wellformedness";
    static final int DEFAULT_MAX_DEPTH = 100;
    static final int DEFAULT_MAX_SIZE = 10_000;

    private static final int SEQUENCE = 0;
    private static final int MAPPING_KEY = 1;
    private static final int MAPPING_VALUE = 2;

    private final int maxDepth;
    private final int maxSize;

    WellformednessValidator(int maxDepth, int maxSize) {
        this.maxDepth = maxDepth;
        this.maxSize = maxSize;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public List<Diagnostic> validate(Document doc) {
        List<Diagnostic> diagnostics = new ArrayList<>();

        if (doc.root() == null) {
            diagnostics.add(new Diagnostic(Severity.ERROR, "null-root", "Document root is null", null, null));
            return diagnostics;
        }

        if (doc.root().isMissingNode() || doc.root().isNull()) {
            diagnostics.add(new Diagnostic(Severity.WARN, "empty-document", "Document is empty", null, null));
        }

        checkDepth(doc.root(), 1, diagnostics);
        checkSize(doc.root(), diagnostics);

        if (doc.sourcePath() != null) {
            Path path = Path.of(doc.sourcePath());
            if (Files.exists(path)) {
                checkRawContent(path, diagnostics);
            }
        }

        return diagnostics;
    }

    void checkDepth(JsonNode node, int currentDepth, List<Diagnostic> diagnostics) {
        if (currentDepth > maxDepth) {
            diagnostics.add(new Diagnostic(
                    Severity.ERROR, "depth-limit-exceeded",
                    "Document exceeds maximum depth of " + maxDepth,
                    null, null));
            return;
        }
        if (node.isObject()) {
            for (Map.Entry<String, JsonNode> field : node.properties()) {
                checkDepth(field.getValue(), currentDepth + 1, diagnostics);
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                checkDepth(child, currentDepth + 1, diagnostics);
            }
        }
    }

    void checkSize(JsonNode root, List<Diagnostic> diagnostics) {
        int size = countNodes(root);
        if (size > maxSize) {
            diagnostics.add(new Diagnostic(
                    Severity.ERROR, "size-limit-exceeded",
                    "Document exceeds maximum size of " + maxSize + " nodes (found " + size + ")",
                    null, null));
        }
    }

    int countNodes(JsonNode node) {
        if (node == null) {
            return 0;
        }
        int count = 1;
        if (node.isObject()) {
            for (Map.Entry<String, JsonNode> field : node.properties()) {
                count += countNodes(field.getValue());
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                count += countNodes(child);
            }
        }
        return count;
    }

    void checkRawContent(Path path, List<Diagnostic> diagnostics) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            checkEncoding(bytes, diagnostics);

            boolean isYaml = isYamlFile(path);
            String content = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);

            if (isYaml) {
                checkTabIndentation(content, diagnostics);
                checkYamlKeys(path, diagnostics);
            }
            checkDuplicateKeys(path, isYaml, diagnostics);
        } catch (IOException e) {
            diagnostics.add(new Diagnostic(
                    Severity.ERROR, "read-error",
                    "Cannot read file: " + e.getMessage(),
                    null, null));
        }
    }

    void checkEncoding(byte[] bytes, List<Diagnostic> diagnostics) {
        if (bytes.length >= 3 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
            diagnostics.add(new Diagnostic(
                    Severity.WARN, "utf8-bom",
                    "File starts with UTF-8 BOM (byte order mark)",
                    null, null));
        }
        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i] & 0xFF;
            if (b >= 0x80) {
                int expectedLen = utf8SequenceLength(b);
                if (expectedLen == 0) {
                    diagnostics.add(new Diagnostic(
                            Severity.ERROR, "invalid-encoding",
                            "Invalid UTF-8 byte at offset " + i,
                            null, null));
                    return;
                }
                if (i + expectedLen > bytes.length) {
                    diagnostics.add(new Diagnostic(
                            Severity.ERROR, "invalid-encoding",
                            "Truncated UTF-8 sequence at offset " + i,
                            null, null));
                    return;
                }
                for (int j = 1; j < expectedLen; j++) {
                    if ((bytes[i + j] & 0xC0) != 0x80) {
                        diagnostics.add(new Diagnostic(
                                Severity.ERROR, "invalid-encoding",
                                "Invalid UTF-8 continuation byte at offset " + (i + j),
                                null, null));
                        return;
                    }
                }
                i += expectedLen - 1;
            }
        }
    }

    int utf8SequenceLength(int leadByte) {
        if ((leadByte & 0xE0) == 0xC0) return 2;
        if ((leadByte & 0xF0) == 0xE0) return 3;
        if ((leadByte & 0xF8) == 0xF0) return 4;
        return 0; // invalid lead byte
    }

    void checkTabIndentation(String content, List<Diagnostic> diagnostics) {
        String[] lines = content.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].startsWith("\t")) {
                diagnostics.add(new Diagnostic(
                        Severity.ERROR, "tab-indentation",
                        "Tab character used for indentation at line " + (i + 1),
                        null, null));
                return;
            }
        }
    }

    void checkYamlKeys(Path path, List<Diagnostic> diagnostics) {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            parseYamlKeys(new Yaml().parse(reader), diagnostics);
        } catch (Exception e) {
            // parse errors are not our concern here
        }
    }

    /**
     * Walks raw YAML events rather than Jackson tokens, because only the event stream retains the
     * scalar style: a quoted ({@code "200"}) or explicitly tagged key is always a string.
     */
    void parseYamlKeys(Iterable<Event> events, List<Diagnostic> diagnostics) {
        Deque<Integer> stack = new ArrayDeque<>();
        for (Event event : events) {
            if (event instanceof CollectionEndEvent) {
                stack.pop();
                continue;
            }
            if (!(event instanceof NodeEvent)) {
                continue;
            }
            Integer state = stack.peek();
            boolean isKey = state != null && state == MAPPING_KEY;
            if (state != null && state != SEQUENCE) {
                stack.pop();
                stack.push(isKey ? MAPPING_VALUE : MAPPING_KEY);
            }
            if (event instanceof MappingStartEvent) {
                stack.push(MAPPING_KEY);
            } else if (event instanceof SequenceStartEvent) {
                stack.push(SEQUENCE);
            } else if (isKey && event instanceof ScalarEvent scalar
                    && scalar.isPlain() && scalar.getTag() == null
                    && isNonStringKey(scalar.getValue())) {
                diagnostics.add(new Diagnostic(
                        Severity.WARN, "non-string-key",
                        "Non-string YAML key: " + scalar.getValue(),
                        null, null));
                return;
            }
        }
    }

    boolean isNonStringKey(String key) {
        if (key == null) return false;
        if ("true".equals(key) || "false".equals(key) || "null".equals(key)) return true;
        try {
            Double.parseDouble(key);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    void checkDuplicateKeys(Path path, boolean isYaml, List<Diagnostic> diagnostics) {
        try {
            JsonFactory factory = isYaml ? new YAMLFactory() : new JsonFactory();
            try (JsonParser parser = factory.createParser(Files.newInputStream(path))) {
                findDuplicateKeys(parser, diagnostics);
            }
        } catch (IOException e) {
            // parse errors handled elsewhere
        }
    }

    void findDuplicateKeys(JsonParser parser, List<Diagnostic> diagnostics) throws IOException {
        List<Set<String>> keyStack = new ArrayList<>();
        while (parser.nextToken() != null) {
            JsonToken token = parser.currentToken();
            if (token == JsonToken.START_OBJECT) {
                keyStack.add(new HashSet<>());
            } else if (token == JsonToken.END_OBJECT) {
                keyStack.removeLast();
            } else if (token == JsonToken.FIELD_NAME) {
                String fieldName = parser.currentName();
                Set<String> currentKeys = keyStack.getLast();
                if (!currentKeys.add(fieldName)) {
                    diagnostics.add(new Diagnostic(
                            Severity.ERROR, "duplicate-key",
                            "Duplicate key: " + fieldName,
                            null, null));
                    return;
                }
            }
        }
    }

    boolean isYamlFile(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".yml") || name.endsWith(".yaml");
    }
}
