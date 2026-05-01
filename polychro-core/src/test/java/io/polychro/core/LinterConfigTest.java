package io.polychro.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LinterConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void defaultsShouldReturnEmptyValidatorsAndNoFailFast() {
        LinterConfig config = LinterConfig.defaults();
        assertTrue(config.validators().isEmpty());
        assertTrue(config.validatorConfigs().isEmpty());
        assertFalse(config.failFast());
        assertEquals("json-schema", config.defaultSchemaValidator());
    }

    @Test
    void loadFromPathShouldParseValidYaml() throws Exception {
        String yaml = """
                validators:
                  - wellformedness
                  - json-schema
                failFast: true
                defaultSchemaValidator: json-structure
                config:
                  json-schema:
                    strict: true
                """;
        Path file = tempDir.resolve(".polychro.yml");
        Files.writeString(file, yaml);

        LinterConfig config = LinterConfig.load(file);

        assertEquals(List.of("wellformedness", "json-schema"), config.validators());
        assertTrue(config.failFast());
        assertEquals("json-structure", config.defaultSchemaValidator());
        assertEquals(Map.of("strict", true), config.validatorConfigs().get("json-schema"));
    }

    @Test
    void loadFromInputStreamShouldParse() {
        String yaml = """
                validators:
                  - ruleset
                failFast: false
                """;
        InputStream is = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
        LinterConfig config = LinterConfig.load(is);

        assertEquals(List.of("ruleset"), config.validators());
        assertFalse(config.failFast());
    }

    @Test
    void loadFromPathShouldThrowOnNonExistentFile() {
        Path nonExistent = tempDir.resolve("missing.yml");
        assertThrows(UncheckedIOException.class, () -> LinterConfig.load(nonExistent));
    }

    @Test
    void loadFromInputStreamShouldThrowOnInvalidYaml() {
        // A stream that throws IOException when read
        InputStream broken = new InputStream() {
            @Override
            public int read() throws java.io.IOException {
                throw new java.io.IOException("simulated");
            }
        };
        assertThrows(UncheckedIOException.class, () -> LinterConfig.load(broken));
    }

    @Test
    void parseShouldReturnDefaultsForNullRoot() {
        LinterConfig config = LinterConfig.parse(null);
        assertTrue(config.validators().isEmpty());
        assertFalse(config.failFast());
    }

    @Test
    void parseShouldReturnDefaultsForEmptyRoot() {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode empty = mapper.createObjectNode();
        LinterConfig config = LinterConfig.parse(empty);
        assertTrue(config.validators().isEmpty());
        assertFalse(config.failFast());
    }

    @Test
    void parseShouldHandleValidatorsNotArray() throws Exception {
        String yaml = """
                validators: "not-an-array"
                """;
        InputStream is = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
        com.fasterxml.jackson.databind.ObjectMapper yamlMapper = new com.fasterxml.jackson.databind.ObjectMapper(
                new com.fasterxml.jackson.dataformat.yaml.YAMLFactory());
        com.fasterxml.jackson.databind.JsonNode root = yamlMapper.readTree(is);
        LinterConfig config = LinterConfig.parse(root);
        assertTrue(config.validators().isEmpty());
    }

    @Test
    void parseShouldHandleConfigNotObject() throws Exception {
        String yaml = """
                config: "not-an-object"
                """;
        InputStream is = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
        com.fasterxml.jackson.databind.ObjectMapper yamlMapper = new com.fasterxml.jackson.databind.ObjectMapper(
                new com.fasterxml.jackson.dataformat.yaml.YAMLFactory());
        com.fasterxml.jackson.databind.JsonNode root = yamlMapper.readTree(is);
        LinterConfig config = LinterConfig.parse(root);
        assertTrue(config.validatorConfigs().isEmpty());
    }

    @Test
    void parseShouldHandleMissingFailFast() throws Exception {
        String yaml = """
                validators:
                  - test
                """;
        InputStream is = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
        com.fasterxml.jackson.databind.ObjectMapper yamlMapper = new com.fasterxml.jackson.databind.ObjectMapper(
                new com.fasterxml.jackson.dataformat.yaml.YAMLFactory());
        com.fasterxml.jackson.databind.JsonNode root = yamlMapper.readTree(is);
        LinterConfig config = LinterConfig.parse(root);
        assertFalse(config.failFast());
    }

    @Test
    void parseShouldDefaultSchemaValidatorToJsonSchema() throws Exception {
        String yaml = """
                validators:
                  - test
                """;
        InputStream is = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
        com.fasterxml.jackson.databind.ObjectMapper yamlMapper = new com.fasterxml.jackson.databind.ObjectMapper(
                new com.fasterxml.jackson.dataformat.yaml.YAMLFactory());
        com.fasterxml.jackson.databind.JsonNode root = yamlMapper.readTree(is);
        LinterConfig config = LinterConfig.parse(root);
        assertEquals("json-schema", config.defaultSchemaValidator());
    }

    @Test
    void constructorShouldHandleNullValidators() {
        LinterConfig config = new LinterConfig(null, null, false, "json-schema");
        assertTrue(config.validators().isEmpty());
        assertTrue(config.validatorConfigs().isEmpty());
    }

    @Test
    void parseShouldReturnDefaultsForNullNode() throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode nullNode = mapper.readTree("null");
        LinterConfig config = LinterConfig.parse(nullNode);
        assertTrue(config.validators().isEmpty());
    }
}
