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

/**
 * Configuration for the {@link Linter}, loaded from {@code .polychro.yml}.
 * <p>
 * Supports enabling/disabling validators and providing per-validator configuration.
 */
public class LinterConfig {

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    private final List<String> validators;
    private final Map<String, Map<String, Object>> validatorConfigs;
    private final boolean failFast;
    private final String defaultSchemaValidator;

    LinterConfig(List<String> validators, Map<String, Map<String, Object>> validatorConfigs,
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
     */
    public static LinterConfig load(Path path) {
        try (InputStream is = Files.newInputStream(path)) {
            return load(is);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load config: " + path, e);
        }
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
            var fields = configNode.fields();
            while (fields.hasNext()) {
                var entry = fields.next();
                Map<String, Object> props = YAML_MAPPER.convertValue(entry.getValue(), Map.class);
                validatorConfigs.put(entry.getKey(), props);
            }
        }

        return new LinterConfig(validators, validatorConfigs, failFast, defaultSchemaValidator);
    }
}
