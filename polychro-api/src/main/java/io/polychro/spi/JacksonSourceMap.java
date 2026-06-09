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
package io.polychro.spi;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * A {@link SourceMap} that indexes the original source location of every scalar, object, and
 * array node in a structured (YAML/JSON) document, keyed by its dot-notation JSONPath
 * (e.g. {@code $.consumes[0].baseUri}).
 *
 * <p>The map is built with a single streaming pass over the raw content using a location-aware
 * Jackson {@link JsonParser}. For each value token the parser exposes a {@link JsonLocation}
 * via {@link JsonParser#currentTokenLocation()}; that location is converted from Jackson's native
 * 1-based line/column to a 0-based {@link SourceRange} (to match Spectral and the LSP convention
 * consumers expect). Lookups for unknown paths return {@code null}, so callers fall back to a null
 * range exactly as before — no regression for unresolvable paths.
 *
 * <p>Jackson reliably reports the <em>start</em> of each token. A precise end location is not
 * always available across formats, so the recorded range uses the token start as both start and
 * (best-effort) end; this is sufficient for editors to place a marker on the offending node.
 *
 * <p><strong>Dotted keys are ambiguous.</strong> Paths are built by joining object keys with
 * {@code '.'}, so a flat key that itself contains a dot (e.g. {@code "x-meta.owner"}) and a nested
 * structure ({@code x-meta: { owner: ... }}) both map to the same path {@code $.x-meta.owner}.
 * When a document contains both, {@link #index} keeps the location of the <em>first</em> token
 * encountered (see the {@code putIfAbsent} guard there). Range resolution for paths that traverse
 * a key containing a dot therefore cannot be guaranteed precise; this is an accepted limitation of
 * the dot-notation keying, not a bug. Lookups still never throw — they resolve to that first
 * location or to {@code null}.
 */
public final class JacksonSourceMap implements SourceMap {

    private final Map<String, SourceRange> rangesByPath;

    private JacksonSourceMap(Map<String, SourceRange> rangesByPath) {
        this.rangesByPath = rangesByPath;
    }

    @Override
    public SourceRange resolve(String path) {
        if (path == null) {
            return null;
        }
        return rangesByPath.get(path);
    }

    /**
     * Build a source map for the given content in the given structured format.
     *
     * @param content the raw document content
     * @param format  the canonical format identifier ({@code "yaml"} or {@code "json"})
     * @return a populated source map, or {@link SourceMap#NONE} if the format is not a structured
     *         text format the parser can locate, or if the content cannot be scanned
     */
    public static SourceMap forContent(String content, String format) {
        JsonFactory factory = switch (format) {
            case "yaml" -> new YAMLFactory();
            case "json" -> new JsonFactory();
            default -> null;
        };
        if (factory == null || content == null) {
            return SourceMap.NONE;
        }
        try {
            return new JacksonSourceMap(index(factory, content, "yaml".equals(format)));
        } catch (IOException e) {
            // A document that cannot be scanned for locations degrades gracefully to no ranges;
            // the well-formedness validator surfaces the parse error separately.
            return SourceMap.NONE;
        }
    }

    private static Map<String, SourceRange> index(JsonFactory factory, String content, boolean yaml)
            throws IOException {
        Map<String, SourceRange> ranges = new HashMap<>();
        try (JsonParser parser = factory.createParser(content)) {
            // Each frame describes the container the parser is currently inside.
            Deque<Frame> stack = new ArrayDeque<>();
            String pendingField = null;
            JsonToken token;
            while ((token = parser.nextToken()) != null) {
                if (token == JsonToken.FIELD_NAME) {
                    pendingField = parser.currentName();
                    continue;
                }
                if (token == JsonToken.END_OBJECT || token == JsonToken.END_ARRAY) {
                    stack.pop();
                    pendingField = null;
                    continue;
                }
                // A value token (scalar, or the opening of an object/array): record its path.
                String path = pathOf(stack, pendingField);
                boolean scalar = token != JsonToken.START_OBJECT && token != JsonToken.START_ARRAY;
                // A given path is claimed by at most one token in a valid document; putIfAbsent
                // guards malformed or dotted-key-colliding input (see class Javadoc) from silently
                // overwriting a prior valid location — the first token encountered wins.
                ranges.putIfAbsent(
                        path, toRange(parser.currentTokenLocation(), content, yaml && scalar));

                if (token == JsonToken.START_OBJECT) {
                    stack.push(new Frame(path, true));
                } else if (token == JsonToken.START_ARRAY) {
                    stack.push(new Frame(path, false));
                }
                pendingField = null;
            }
        }
        return ranges;
    }

    /**
     * Compute the dot-notation path of the value the parser is currently positioned on, given the
     * enclosing container stack and the field name most recently read (if the parent is an object).
     */
    private static String pathOf(Deque<Frame> stack, String pendingField) {
        if (stack.isEmpty()) {
            return "$";
        }
        Frame parent = stack.peek();
        if (parent.isObject) {
            return parent.path + "." + pendingField;
        }
        return parent.path + "[" + (parent.nextIndex++) + "]";
    }

    private static SourceRange toRange(JsonLocation location, String content, boolean yamlScalar) {
        // Jackson reports 1-based line/column; Polychro ranges are 0-based to match Spectral and
        // the LSP convention consumers expect (vscode.Position is 0-based, copied 1:1). Clamp at 0
        // so a synthetic location (-1) never produces a negative.
        int line = Math.max(location.getLineNr() - 1, 0);
        int column = Math.max(location.getColumnNr() - 1, 0);
        if (!yamlScalar) {
            // Containers (object/array) and non-YAML scalars carry a best-effort point marker:
            // the end collapses onto the start. Iso-Spectral end ranges are YAML-scalar only (#32);
            // JSON end ranges are tracked separately (#34).
            return new SourceRange(line, column, line, column);
        }
        int[] end = endOfYamlScalar(content, line, column);
        return new SourceRange(line, column, end[0], end[1]);
    }

    /**
     * Compute the 0-based, end-exclusive end position of the YAML scalar whose raw source begins at
     * {@code (startLine, startColumn)}. The end is the position just past the last source character
     * of the scalar — opening and closing quotes included for quoted scalars, escape sequences
     * counted by their physical source length, and the full span of a multi-line block scalar — so
     * the range reproduces the one Spectral emits for the same bytes.
     *
     * @return a two-element array {@code [endLine, endColumn]} (both 0-based, end exclusive)
     */
    static int[] endOfYamlScalar(String content, int startLine, int startColumn) {
        String[] lines = content.split("\n", -1);
        if (startLine < 0 || startLine >= lines.length) {
            return new int[] {startLine, startColumn};
        }
        String line = lines[startLine];
        if (startColumn < 0 || startColumn > line.length()) {
            return new int[] {startLine, startColumn};
        }
        char first = startColumn < line.length() ? line.charAt(startColumn) : '\0';
        if (first == '"' || first == '\'') {
            return endOfQuotedScalar(line, startLine, startColumn, first);
        }
        if (first == '>' || first == '|') {
            return endOfBlockScalar(lines, startLine, startColumn);
        }
        return endOfPlainScalar(line, startLine, startColumn);
    }

    /**
     * End of a quoted scalar: advance to the matching closing quote (honouring {@code \"}/{@code \\}
     * escapes for double quotes and {@code ''} doubling for single quotes) and return the position
     * just past it. Falls back to end-of-line if the closing quote is absent.
     */
    static int[] endOfQuotedScalar(String line, int startLine, int startColumn, char quote) {
        int i = startColumn + 1;
        while (i < line.length()) {
            char c = line.charAt(i);
            if (quote == '"' && c == '\\' && i + 1 < line.length()) {
                i += 2;
                continue;
            }
            if (quote == '\'' && c == '\'' && i + 1 < line.length() && line.charAt(i + 1) == '\'') {
                i += 2;
                continue;
            }
            if (c == quote) {
                return new int[] {startLine, i + 1};
            }
            i++;
        }
        return new int[] {startLine, line.length()};
    }

    /**
     * End of a plain (unquoted) scalar: the value runs from the start column to the last non-blank
     * character before a trailing comment ({@code " #"}) or end of line. Trailing whitespace is
     * excluded so the range hugs the value exactly, iso-Spectral.
     */
    static int[] endOfPlainScalar(String line, int startLine, int startColumn) {
        int end = line.length();
        for (int i = startColumn; i < line.length(); i++) {
            if (line.charAt(i) == '#' && i > startColumn && Character.isWhitespace(line.charAt(i - 1))) {
                end = i;
                break;
            }
        }
        while (end > startColumn && Character.isWhitespace(line.charAt(end - 1))) {
            end--;
        }
        return new int[] {startLine, end};
    }

    /**
     * End of a block scalar ({@code >} or {@code |}): the indicator sits on {@code startLine}; the
     * content is the run of more-indented lines that follow. The end is just past the last non-blank
     * character of the last content line, ignoring Jackson's tendency to overshoot to the next key.
     */
    static int[] endOfBlockScalar(String[] lines, int startLine, int startColumn) {
        int keyIndent = indentOf(lines[startLine]);
        int lastLine = startLine;
        int lastCol = startColumn + 1;
        for (int i = startLine + 1; i < lines.length; i++) {
            String l = lines[i];
            if (l.isBlank()) {
                continue;
            }
            if (indentOf(l) <= keyIndent) {
                break;
            }
            int trimmed = l.length();
            // l is non-blank (blank lines are skipped above), so a non-whitespace char
            // always exists before trimmed hits 0 — no lower-bound guard needed.
            while (Character.isWhitespace(l.charAt(trimmed - 1))) {
                trimmed--;
            }
            lastLine = i;
            // include the trailing newline folded into the block scalar (iso-Spectral).
            lastCol = trimmed + 1;
        }
        return new int[] {lastLine, lastCol};
    }

    /** Number of leading spaces on a line (its indentation depth). */
    static int indentOf(String line) {
        int n = 0;
        while (n < line.length() && line.charAt(n) == ' ') {
            n++;
        }
        return n;
    }

    /**
     * A container the parser has descended into. {@code path} is the full dot-notation path of the
     * container itself; {@code nextIndex} is the index the next array element will take.
     */
    private static final class Frame {
        final String path;
        final boolean isObject;
        int nextIndex;

        Frame(String path, boolean isObject) {
            this.path = path;
            this.isObject = isObject;
        }
    }
}
