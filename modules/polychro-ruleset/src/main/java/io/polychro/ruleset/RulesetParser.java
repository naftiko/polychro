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
package io.polychro.ruleset;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.polychro.ruleset.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses Spectral-format ruleset YAML files into {@link Ruleset} records.
 */
public class RulesetParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(RulesetParser.class);
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());
    public static final List<String> SUPPORTED_SCRIPT_EXTENSIONS = List.of(".js", ".py", ".groovy");

    public enum RulesetSource {
        FILE_SYSTEM, CLASSPATH
    }

    /**
     * Parse a ruleset from a file path.
     *
     * @param path the path to the ruleset YAML
     * @return the parsed Ruleset
     * @throws UncheckedIOException if the file cannot be read or parsed
     * @throws RulesetParseException if the content is not a valid ruleset
     */
    public Ruleset parse(Path path) {
        try {
            String content = FileUtils.getFileContentFromFileSystem(path);
            JsonNode root = YAML_MAPPER.readTree(content);
            return parseNode(root, resolveBaseDir(path).toString(), RulesetSource.FILE_SYSTEM);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read ruleset: " + path, e);
        }
    }

    /**
     * Resolves the directory to use as the base for locating a ruleset's custom function scripts.
     *
     * <p>Normally this is the ruleset file's parent directory. A bare relative filename (e.g.
     * {@code ruleset.yml}, the shape typed on {@code --ruleset ruleset.yml}) resolves safely via
     * {@link Path#toAbsolutePath()} — its parent becomes the current working directory. The only
     * case with no parent is a path that already denotes a filesystem root (e.g. {@code /});
     * {@link Path#getParent()} then returns {@code null}, so this falls back to the root itself
     * rather than letting the caller's {@code .toString()} throw a {@link NullPointerException}
     * (PR #128 review, finding #1).
     *
     * @param path the ruleset file path (relative or absolute)
     * @return the resolved absolute base directory, never {@code null}
     */
    static Path resolveBaseDir(Path path) {
        Path absolute = path.toAbsolutePath();
        Path parent = absolute.getParent();
        return parent != null ? parent : absolute;
    }

    /**
     * Parse a ruleset from a YAML string.
     *
     * @param yaml the ruleset YAML content
     * @return the parsed Ruleset
     * @throws RulesetParseException if the content is not a valid ruleset
     */
    public Ruleset parse(String yaml) {
        try {
            JsonNode root = YAML_MAPPER.readTree(yaml);
            return parseNode(root, ".", RulesetSource.FILE_SYSTEM);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to parse ruleset YAML", e);
        }
    }

    /**
     * A ruleset parsing method capable of parsing functions from both the file system and classpath.
     * @param rulesetPath           the ruleset's path.
     * @param functionsBasePath     the functions' base path.
     * @param rulesetSource         the parsing mode.
     * @return                      the parsed ruleset.
     * @throws UncheckedIOException in case an error occurs while reading the ruleset's content.
     */
    public Ruleset parse(String rulesetPath, String functionsBasePath, RulesetSource rulesetSource) {
        return switch (rulesetSource) {
            case FILE_SYSTEM -> {
                Path path = Path.of(rulesetPath);
                try {
                    String content = FileUtils.getFileContentFromFileSystem(path);
                    JsonNode root = YAML_MAPPER.readTree(content);
                    yield parseNode(root, resolveBaseDir(path).resolve(functionsBasePath).toString(),
                            RulesetSource.FILE_SYSTEM);
                } catch (IOException e) {
                    throw new UncheckedIOException("Failed to read ruleset: " + path, e);
                }
            }
            case CLASSPATH -> {
                try {
                    String content = FileUtils.getFileContentFromClasspath(rulesetPath);
                    JsonNode root = YAML_MAPPER.readTree(content);
                    yield parseNode(root, functionsBasePath, RulesetSource.CLASSPATH);
                } catch (IOException e) {
                    throw new UncheckedIOException("Failed to read ruleset: " + rulesetPath, e);
                }
            }
        };
    }

    private Ruleset parseNode(JsonNode root, String functionsBasePath, RulesetSource parseMode) {
        if (!root.isObject()) {
            throw new RulesetParseException("Ruleset must be a YAML object");
        }

        List<String> extendsRefs = parseExtends(root.get("extends"));
        Map<String, String> aliases = parseAliases(root.get("aliases"));
        List<RulesetOverride> overrides = parseOverrides(root.get("overrides"));
        List<String> formats = parseStringList(root.get("formats"));
        List<Function> functions = parseFunctions(root, functionsBasePath, parseMode);
        String documentationUrl = textOrNull(root.get("documentationUrl"));
        Map<String, Rule> rules = parseRules(root.get("rules"));

        return new Ruleset(extendsRefs, aliases, overrides, formats,
                functions, rules, documentationUrl);
    }

    private List<Function> parseFunctions(JsonNode root, String functionsBasePath, RulesetSource parseMode) {
        String functionsDir = textOrNull(root.get("functionsDir"));
        List<String> functionNames = parseStringList(root.get("functions"));
        if (functionsDir == null) {
            return Collections.emptyList();
        }

        List<Function> functions = new ArrayList<>(functionNames.size());
        for (String functionName : functionNames) {
            Function function = getFunction(functionsBasePath, functionsDir, functionName, parseMode);
            if (function != null) {
                functions.add(function);
            } else {
                LOGGER.warn("Function '{}' sourceCode could not be retrieved", functionName);
            }
        }

        return functions;
    }

    private Function getFunction(String functionsBasePath,
                                 String functionsDir,
                                 String function,
                                 RulesetSource parseMode) {
        return switch (parseMode) {
            case FILE_SYSTEM -> {
                for (String extension : SUPPORTED_SCRIPT_EXTENSIONS) {
                    String filename = function + extension;
                    Path source = Path.of(functionsBasePath).resolve(functionsDir).resolve(filename);
                    try {
                        yield new Function(filename, function, FileUtils.getFileContentFromFileSystem(source));
                    } catch (IOException e) {
                        LOGGER.error("An error has occurred while reading function sourceCode at: {}", source, e);
                    }
                }
                yield null;
            }
            case CLASSPATH -> {
                for (String extension : SUPPORTED_SCRIPT_EXTENSIONS) {
                    String filename = function + extension;
                    String source = functionsBasePath + "/" + functionsDir + "/" + filename;
                    try {
                        String content = FileUtils.getFileContentFromClasspath(source);
                        yield new Function(filename, function, content);
                    } catch (IOException e) {
                        LOGGER.error("An error has occurred while reading function sourceCode at: {}", source, e);
                    }
                }
                yield null;
            }
        };
    }

    private List<String> parseExtends(JsonNode node) {
        if (node == null || node.isNull()) {
            return List.of();
        }
        if (node.isTextual()) {
            return List.of(node.asText());
        }
        if (node.isArray()) {
            List<String> result = new ArrayList<>();
            for (JsonNode item : node) {
                if (item.isTextual()) {
                    result.add(item.asText());
                } else if (item.isArray() && item.size() == 2) {
                    // ["spectral:oas", "off"] format
                    result.add(item.get(0).asText());
                }
            }
            return result;
        }
        return List.of();
    }

    private Map<String, String> parseAliases(JsonNode node) {
        if (node == null || !node.isObject()) {
            return Map.of();
        }
        Map<String, String> aliases = new LinkedHashMap<>();
        var fields = node.properties();
        for (var entry : fields) {
            JsonNode value = entry.getValue();
            if (value.isTextual()) {
                aliases.put(entry.getKey(), value.asText());
            } else if (value.isObject() && value.has("targets")) {
                // Complex alias format — store the first target's given
                JsonNode targets = value.get("targets");
                if (targets.isArray() && !targets.isEmpty()) {
                    JsonNode firstTarget = targets.get(0);
                    if (firstTarget.has("given")) {
                        JsonNode given = firstTarget.get("given");
                        if (given.isTextual()) {
                            aliases.put(entry.getKey(), given.asText());
                        } else if (given.isArray() && !given.isEmpty()) {
                            aliases.put(entry.getKey(), given.get(0).asText());
                        }
                    }
                }
            }
        }
        return aliases;
    }

    private List<String> parseStringList(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (JsonNode item : node) {
            if (item.isTextual()) {
                result.add(item.asText());
            }
        }
        return result;
    }

    private Map<String, Rule> parseRules(JsonNode node) {
        if (node == null || !node.isObject()) {
            return Map.of();
        }
        Map<String, Rule> rules = new LinkedHashMap<>();
        var fields = node.properties();
        for (var entry : fields) {
            String ruleName = entry.getKey();
            JsonNode ruleNode = entry.getValue();
            rules.put(ruleName, parseRule(ruleName, ruleNode));
        }
        return rules;
    }

    private Rule parseRule(String name, JsonNode node) {
        if (!node.isObject()) {
            throw new RulesetParseException("Rule '" + name + "' must be an object");
        }

        String message = textOrNull(node.get("message"));
        String description = textOrNull(node.get("description"));
        String severity = parseSeverity(node.get("severity"));
        boolean recommended = node.has("recommended") ? node.get("recommended").asBoolean(true) : true;
        List<String> formats = node.has("formats") ? parseStringList(node.get("formats")) : null;
        String documentationUrl = textOrNull(node.get("documentationUrl"));
        List<String> given = parseGiven(node.get("given"));
        List<RuleAction> then = parseThen(node.get("then"));

        return new Rule(name, message, description, severity, recommended, formats, documentationUrl, given, then);
    }

    private List<String> parseGiven(JsonNode node) {
        if (node == null || node.isNull()) {
            return List.of();
        }
        if (node.isTextual()) {
            return List.of(node.asText());
        }
        if (node.isArray()) {
            List<String> result = new ArrayList<>();
            for (JsonNode item : node) {
                result.add(item.asText());
            }
            return result;
        }
        return List.of();
    }

    private List<RuleAction> parseThen(JsonNode node) {
        if (node == null || node.isNull()) {
            return List.of();
        }
        if (node.isObject()) {
            return List.of(parseAction(node));
        }
        if (node.isArray()) {
            List<RuleAction> actions = new ArrayList<>();
            for (JsonNode item : node) {
                actions.add(parseAction(item));
            }
            return actions;
        }
        return List.of();
    }

    private RuleAction parseAction(JsonNode node) {
        if (!node.isObject()) {
            throw new RulesetParseException("Rule action must be an object");
        }

        String field = textOrNull(node.get("field"));
        String functionName = null;
        Map<String, Object> functionOptions = Map.of();

        if (node.has("function")) {
            functionName = node.get("function").asText();
        }
        if (node.has("functionOptions")) {
            functionOptions = parseOptions(node.get("functionOptions"));
        }

        return new RuleAction(field, functionName, functionOptions);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseOptions(JsonNode node) {
        if (!node.isObject()) {
            return Map.of();
        }
        return YAML_MAPPER.convertValue(node, Map.class);
    }

    private List<RulesetOverride> parseOverrides(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<RulesetOverride> overrides = new ArrayList<>();
        for (JsonNode item : node) {
            if (item.isObject()) {
                overrides.add(parseOverride(item));
            }
        }
        return overrides;
    }

    private RulesetOverride parseOverride(JsonNode node) {
        List<String> files = parseStringList(node.get("files"));
        Map<String, Rule> rules = parseRules(node.get("rules"));
        Map<String, String> aliases = parseAliases(node.get("aliases"));
        List<String> formats = node.has("formats") ? parseStringList(node.get("formats")) : null;
        return new RulesetOverride(files, rules, aliases, formats);
    }

    private String parseSeverity(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        // YAML interprets bare 'off' as boolean false
        if (node.isBoolean() && !node.asBoolean()) {
            return "off";
        }
        return node.asText();
    }

    private String textOrNull(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return node.asText();
    }
}
