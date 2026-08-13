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

    // Regression tests for the bug reported against `polychro lint --config <dir>/.polychro.yml
    // <dir>/file.yml` executed from *outside* <dir>: schemaPath (and the other well-known
    // path-valued config keys) must resolve relative to the .polychro.yml file's own directory,
    // not the process's current working directory.

    @Test
    void loadFromPathShouldResolveSchemaPathRelativeToConfigFileDirectory() throws Exception {
        Path configDir = tempDir.resolve("files");
        Files.createDirectories(configDir);
        Files.writeString(configDir.resolve("openapi-schema.json"), "{\"type\": \"object\"}");

        String yaml = """
                validators: []
                config:
                  json-schema:
                    schemaPath: openapi-schema.json
                """;
        Path configFile = configDir.resolve(".polychro.yml");
        Files.writeString(configFile, yaml);

        LinterConfig config = LinterConfig.load(configFile);

        String resolvedSchemaPath = (String) config.validatorConfigs().get("json-schema").get("schemaPath");
        assertEquals(
                configDir.resolve("openapi-schema.json").toAbsolutePath().normalize().toString(),
                resolvedSchemaPath,
                "schemaPath must be resolved against the config file's directory, "
                        + "not the process CWD");
    }

    @Test
    void loadFromPathShouldLeaveSchemaPathUnresolvedWhenFileDoesNotExistNextToConfig() throws Exception {
        // No openapi-schema.json is created next to the config file — the classpath-resource
        // fallback in JsonSchemaValidatorFactory must remain reachable, so the raw value must
        // be preserved unchanged rather than rewritten to a non-existent absolute path.
        String yaml = """
                validators: []
                config:
                  json-schema:
                    schemaPath: schemas/person-schema.json
                """;
        Path configFile = tempDir.resolve(".polychro.yml");
        Files.writeString(configFile, yaml);

        LinterConfig config = LinterConfig.load(configFile);

        assertEquals(
                "schemas/person-schema.json",
                config.validatorConfigs().get("json-schema").get("schemaPath"));
    }

    @Test
    void loadFromPathShouldNotRewriteAbsoluteSchemaPath() throws Exception {
        Path schemaFile = tempDir.resolve("schema.json");
        Files.writeString(schemaFile, "{\"type\": \"object\"}");

        String yaml = """
                validators: []
                config:
                  json-schema:
                    schemaPath: %s
                """.formatted(schemaFile.toString());
        Path configDir = tempDir.resolve("nested");
        Files.createDirectories(configDir);
        Path configFile = configDir.resolve(".polychro.yml");
        Files.writeString(configFile, yaml);

        LinterConfig config = LinterConfig.load(configFile);

        assertEquals(
                schemaFile.toString(),
                config.validatorConfigs().get("json-schema").get("schemaPath"));
    }

    @Test
    void loadFromPathShouldResolveRulesetPathRelativeToConfigFileDirectory() throws Exception {
        Path configDir = tempDir.resolve("files");
        Files.createDirectories(configDir);
        Files.writeString(configDir.resolve("custom-rules.yml"), """
                rules:
                  always-pass:
                    message: "info.name is present"
                    severity: info
                    given: "$"
                    then:
                      field: "name"
                      function: defined
                """);

        String yaml = """
                validators:
                  - ruleset
                config:
                  ruleset:
                    rulesetPath: custom-rules.yml
                """;
        Path configFile = configDir.resolve(".polychro.yml");
        Files.writeString(configFile, yaml);

        LinterConfig config = LinterConfig.load(configFile);

        String resolvedRulesetPath = (String) config.validatorConfigs().get("ruleset").get("rulesetPath");
        assertEquals(
                configDir.resolve("custom-rules.yml").toAbsolutePath().normalize().toString(),
                resolvedRulesetPath);
    }

    @Test
    void loadFromPathShouldSkipBlankPathConfigValue() throws Exception {
        // Covers the `stringValue.isBlank()` branch of resolveRelativePaths: a blank
        // schemaPath must be left untouched (skipped via `continue`) rather than resolved
        // into a directory path, which would be nonsensical.
        String yaml = """
                validators: []
                config:
                  json-schema:
                    schemaPath: ""
                """;
        Path configFile = tempDir.resolve(".polychro.yml");
        Files.writeString(configFile, yaml);

        LinterConfig config = LinterConfig.load(configFile);

        assertEquals("", config.validatorConfigs().get("json-schema").get("schemaPath"));
    }

    @Test
    void loadFromPathShouldResolveMultiplePathKeysWithinTheSameValidatorConfigBlock() throws Exception {
        // Covers the `resolvedProps == null` branch of resolveRelativePaths for its FALSE
        // case: once the first matching path key has allocated the copy-on-write map, a
        // second matching key (checkov's customCheckDir alongside checkovPath) in the same
        // validator config block must reuse it rather than re-allocating or being skipped.
        Path configDir = tempDir.resolve("files");
        Files.createDirectories(configDir);
        Path customCheckDir = configDir.resolve("custom-checks");
        Files.createDirectories(customCheckDir);
        Path checkovBinDir = configDir.resolve("bin");
        Files.createDirectories(checkovBinDir);
        Path checkovBin = checkovBinDir.resolve("checkov");
        Files.writeString(checkovBin, "#!/bin/sh\n");

        String yaml = """
                validators: []
                config:
                  checkov:
                    checkovPath: bin/checkov
                    customCheckDir: custom-checks
                """;
        Path configFile = configDir.resolve(".polychro.yml");
        Files.writeString(configFile, yaml);

        LinterConfig config = LinterConfig.load(configFile);

        Map<String, Object> checkovConfig = config.validatorConfigs().get("checkov");
        assertEquals(
                checkovBin.toAbsolutePath().normalize().toString(),
                checkovConfig.get("checkovPath"));
        assertEquals(
                customCheckDir.toAbsolutePath().normalize().toString(),
                checkovConfig.get("customCheckDir"));
    }

    // Direct unit tests for the package-private resolveRelativePaths(Map, Path) helper —
    // made package-private (was private) per AGENTS.md's Method Visibility convention
    // (PR #126 review) so this non-trivial logic (multiple branches, copy-on-write map
    // allocation) can be exercised directly instead of only indirectly through load(Path).

    @Test
    void resolveRelativePathsShouldRewriteRelativePathThatExistsNextToConfigDir() throws Exception {
        Path configDir = tempDir.resolve("files");
        Files.createDirectories(configDir);
        Files.writeString(configDir.resolve("schema.json"), "{}");

        Map<String, Object> resolved = LinterConfig.resolveRelativePaths(
                Map.of("schemaPath", "schema.json"), configDir);

        assertEquals(
                configDir.resolve("schema.json").toAbsolutePath().normalize().toString(),
                resolved.get("schemaPath"));
    }

    @Test
    void resolveRelativePathsShouldReturnSameInstanceWhenNoKeyMatches() {
        Map<String, Object> props = Map.of("unrelatedKey", "value");

        Map<String, Object> resolved = LinterConfig.resolveRelativePaths(props, tempDir);

        assertSame(props, resolved, "no PATH_CONFIG_KEYS entry present — the original map "
                + "must be returned unchanged, not a copy");
    }
}
