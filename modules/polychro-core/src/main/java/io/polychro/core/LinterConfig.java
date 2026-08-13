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
package io.polychro.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Configuration for the {@link Linter}, loaded from {@code .polychro.yml}.
 * <p>
 * Supports enabling/disabling validators and providing per-validator configuration.
 */
public class LinterConfig {

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    /**
     * Config keys, across all built-in validator factories, whose value is a filesystem path
     * ({@code schemaPath} for {@code json-schema}/{@code json-structure}, {@code rulesetPath} for
     * {@code ruleset}, {@code customCheckDir} and {@code checkovPath} for {@code checkov}). When
     * the config is loaded from a {@code .polychro.yml} file, relative values for these keys are
     * resolved against the config file's own directory rather than the process CWD (see
     * {@link #resolveRelativePaths}).
     */
    private static final Set<String> PATH_CONFIG_KEYS = Set.of(
            "schemaPath", "rulesetPath", "customCheckDir", "checkovPath");

    private final List<String> validators;
    private final Map<String, Map<String, Object>> validatorConfigs;
    private final boolean failFast;
    private final String defaultSchemaValidator;

    public LinterConfig(List<String> validators, Map<String, Map<String, Object>> validatorConfigs,
                 boolean failFast, String defaultSchemaValidator) {
        this.validators = validators != null ? List.copyOf(validators) : List.of();
        this.validatorConfigs = validatorConfigs != null ? Map.copyOf(validatorConfigs) : Map.of();
        this.failFast = failFast;
        this.defaultSchemaValidator = defaultSchemaValidator;
    }

    public List<String> validators() {
        return validators;
    }

    public Map<String, Map<String, Object>> validatorConfigs() {
        return validatorConfigs;
    }

    public boolean failFast() {
        return failFast;
    }

    public String defaultSchemaValidator() {
        return defaultSchemaValidator;
    }

    /**
     * Load configuration from a YAML file path.
     * <p>
     * Filesystem-path config values ({@link #PATH_CONFIG_KEYS}, e.g. {@code schemaPath},
     * {@code rulesetPath}) that are relative are resolved against {@code path}'s parent
     * directory (the directory containing the config file), not the process's current
     * working directory — see {@link #resolveRelativePaths}. This mirrors how
     * {@code extends} entries in a ruleset file are already resolved relative to the
     * ruleset itself.
     */
    public static LinterConfig load(Path path) {
        try (InputStream is = Files.newInputStream(path)) {
            LinterConfig config = load(is);
            // path was just opened successfully as a regular file, so its absolute, normalized
            // form always has a parent directory (the only path with no parent is the
            // filesystem root itself, which cannot be opened as a readable file). No null-check
            // is needed here.
            Path configDir = path.toAbsolutePath().normalize().getParent();
            return resolveRelativePaths(config, configDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load config: " + path, e);
        }
    }

    /**
     * Return a copy of {@code config} where relative values of the well-known
     * {@link #PATH_CONFIG_KEYS} are rewritten to be resolved against {@code configDir} —
     * the directory containing the {@code .polychro.yml} file — instead of the process's
     * current working directory.
     * <p>
     * A key is only rewritten when the resulting file actually exists next to the config
     * file; otherwise the original value is left untouched, so values intended to be
     * resolved against the CWD (the previous behavior) or a classpath resource keep
     * working exactly as before.
     */
    static LinterConfig resolveRelativePaths(LinterConfig config, Path configDir) {
        Map<String, Map<String, Object>> resolvedConfigs = new LinkedHashMap<>();
        for (var entry : config.validatorConfigs().entrySet()) {
            resolvedConfigs.put(entry.getKey(), resolveRelativePaths(entry.getValue(), configDir));
        }
        return new LinterConfig(
                config.validators(), resolvedConfigs, config.failFast(), config.defaultSchemaValidator());
    }

    static Map<String, Object> resolveRelativePaths(Map<String, Object> props, Path configDir) {
        Map<String, Object> resolvedProps = null;
        for (String key : PATH_CONFIG_KEYS) {
            Object value = props.get(key);
            if (!(value instanceof String stringValue) || stringValue.isBlank()) {
                continue;
            }
            Path candidate = Path.of(stringValue);
            if (candidate.isAbsolute()) {
                continue;
            }
            Path resolved = configDir.resolve(candidate).normalize();
            if (Files.exists(resolved)) {
                if (resolvedProps == null) {
                    resolvedProps = new LinkedHashMap<>(props);
                }
                resolvedProps.put(key, resolved.toString());
            }
        }
        return resolvedProps != null ? resolvedProps : props;
    }

    /**
     * Load configuration from an input stream.
     */
    public static LinterConfig load(InputStream is) {
        try {
            JsonNode root = YAML_MAPPER.readTree(is);
            return parse(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to parse config YAML", e);
        }
    }

    /**
     * Return a default configuration (all discovered validators, no fail-fast).
     */
    public static LinterConfig defaults() {
        return new LinterConfig(List.of(), Map.of(), false, "json-schema");
    }

    @SuppressWarnings("unchecked")
    static LinterConfig parse(JsonNode root) {
        if (root == null || root.isNull() || root.isEmpty()) {
            return defaults();
        }

        List<String> validators = List.of();
        if (root.has("validators") && root.get("validators").isArray()) {
            var validatorsNode = root.get("validators");
            var validatorList = new java.util.ArrayList<String>();
            for (JsonNode item : validatorsNode) {
                validatorList.add(item.asText());
            }
            validators = validatorList;
        }

        boolean failFast = root.has("failFast") && root.get("failFast").asBoolean(false);

        String defaultSchemaValidator = "json-schema";
        if (root.has("defaultSchemaValidator")) {
            defaultSchemaValidator = root.get("defaultSchemaValidator").asText("json-schema");
        }

        Map<String, Map<String, Object>> validatorConfigs = new LinkedHashMap<>();
        if (root.has("config") && root.get("config").isObject()) {
            var configNode = root.get("config");
            var fields = configNode.properties();
            for (var entry : fields) {
                Map<String, Object> props = YAML_MAPPER.convertValue(entry.getValue(), Map.class);
                validatorConfigs.put(entry.getKey(), props);
            }
        }

        return new LinterConfig(validators, validatorConfigs, failFast, defaultSchemaValidator);
    }
}
