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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RulesetParserTest {

    private final RulesetParser parser = new RulesetParser();

    @Test
    void parseShouldHandleFullRuleset() {
        String yaml = """
                extends:
                  - spectral:oas
                aliases:
                  PathItem: "$.paths[*]"
                formats:
                  - oas3
                functionsDir: ./functions
                functions:
                  - myCustomFn
                documentationUrl: https://docs.example.com
                rules:
                  my-rule:
                    message: Something is wrong
                    description: A detailed description
                    severity: error
                    recommended: true
                    given: "$.info"
                    then:
                      function: truthy
                      field: title
                """;

        Ruleset ruleset = parser.parse(yaml);

        assertEquals(List.of("spectral:oas"), ruleset.extendsRefs());
        assertEquals(Map.of("PathItem", "$.paths[*]"), ruleset.aliases());
        assertEquals(List.of("oas3"), ruleset.formats());
        assertEquals("./functions", ruleset.functionsDir());
        assertEquals(List.of("myCustomFn"), ruleset.functions());
        assertEquals("https://docs.example.com", ruleset.documentationUrl());
        assertEquals(1, ruleset.rules().size());

        Rule rule = ruleset.rules().get("my-rule");
        assertEquals("my-rule", rule.name());
        assertEquals("Something is wrong", rule.message());
        assertEquals("A detailed description", rule.description());
        assertEquals("error", rule.severity());
        assertTrue(rule.recommended());
        assertEquals(List.of("$.info"), rule.given());
        assertEquals(1, rule.then().size());
        assertEquals("truthy", rule.then().get(0).functionName());
        assertEquals("title", rule.then().get(0).field());
    }

    @Test
    void parseShouldHandleEmptyRules() {
        String yaml = """
                rules: {}
                """;

        Ruleset ruleset = parser.parse(yaml);
        assertTrue(ruleset.rules().isEmpty());
    }

    @Test
    void parseShouldHandleMissingExtends() {
        String yaml = """
                rules:
                  test-rule:
                    given: "$"
                    then:
                      function: truthy
                """;

        Ruleset ruleset = parser.parse(yaml);
        assertTrue(ruleset.extendsRefs().isEmpty());
    }

    @Test
    void parseShouldHandleFunctionsDirPresent() {
        String yaml = """
                functionsDir: ./custom-functions
                rules: {}
                """;

        Ruleset ruleset = parser.parse(yaml);
        assertEquals("./custom-functions", ruleset.functionsDir());
    }

    @Test
    void parseShouldHandleFunctionsDirAbsent() {
        String yaml = """
                rules: {}
                """;

        Ruleset ruleset = parser.parse(yaml);
        assertNull(ruleset.functionsDir());
    }

    @Test
    void parseShouldHandleSingleThen() {
        String yaml = """
                rules:
                  test-rule:
                    given: "$"
                    then:
                      function: defined
                """;

        Ruleset ruleset = parser.parse(yaml);
        Rule rule = ruleset.rules().get("test-rule");
        assertEquals(1, rule.then().size());
        assertEquals("defined", rule.then().get(0).functionName());
    }

    @Test
    void parseShouldHandleArrayThen() {
        String yaml = """
                rules:
                  test-rule:
                    given: "$"
                    then:
                      - function: truthy
                        field: name
                      - function: length
                        functionOptions:
                          min: 1
                """;

        Ruleset ruleset = parser.parse(yaml);
        Rule rule = ruleset.rules().get("test-rule");
        assertEquals(2, rule.then().size());
        assertEquals("truthy", rule.then().get(0).functionName());
        assertEquals("name", rule.then().get(0).field());
        assertEquals("length", rule.then().get(1).functionName());
        assertEquals(1, rule.then().get(1).functionOptions().get("min"));
    }

    @Test
    void parseShouldHandleAllSeverityLevels() {
        String yaml = """
                rules:
                  error-rule:
                    severity: error
                    given: "$"
                    then:
                      function: truthy
                  warn-rule:
                    severity: warn
                    given: "$"
                    then:
                      function: truthy
                  info-rule:
                    severity: info
                    given: "$"
                    then:
                      function: truthy
                  hint-rule:
                    severity: hint
                    given: "$"
                    then:
                      function: truthy
                  off-rule:
                    severity: "off"
                    given: "$"
                    then:
                      function: truthy
                """;

        Ruleset ruleset = parser.parse(yaml);
        assertEquals("error", ruleset.rules().get("error-rule").severity());
        assertEquals("warn", ruleset.rules().get("warn-rule").severity());
        assertEquals("info", ruleset.rules().get("info-rule").severity());
        assertEquals("hint", ruleset.rules().get("hint-rule").severity());
        assertEquals("off", ruleset.rules().get("off-rule").severity());
    }

    @Test
    void parseShouldHandleRecommendedFlag() {
        String yaml = """
                rules:
                  recommended-rule:
                    recommended: true
                    given: "$"
                    then:
                      function: truthy
                  not-recommended:
                    recommended: false
                    given: "$"
                    then:
                      function: truthy
                  default-recommended:
                    given: "$"
                    then:
                      function: truthy
                """;

        Ruleset ruleset = parser.parse(yaml);
        assertTrue(ruleset.rules().get("recommended-rule").recommended());
        assertFalse(ruleset.rules().get("not-recommended").recommended());
        assertTrue(ruleset.rules().get("default-recommended").recommended());
    }

    @Test
    void parseShouldHandleGivenAsSingleString() {
        String yaml = """
                rules:
                  test-rule:
                    given: "$.info.title"
                    then:
                      function: truthy
                """;

        Ruleset ruleset = parser.parse(yaml);
        assertEquals(List.of("$.info.title"), ruleset.rules().get("test-rule").given());
    }

    @Test
    void parseShouldHandleGivenAsArray() {
        String yaml = """
                rules:
                  test-rule:
                    given:
                      - "$.info.title"
                      - "$.info.description"
                    then:
                      function: truthy
                """;

        Ruleset ruleset = parser.parse(yaml);
        assertEquals(List.of("$.info.title", "$.info.description"), ruleset.rules().get("test-rule").given());
    }

    @Test
    void parseShouldThrowOnMalformedYaml() {
        String yaml = "not: valid: yaml: [[[";

        assertThrows(java.io.UncheckedIOException.class, () -> parser.parse(yaml));
    }

    @Test
    void parseShouldThrowOnNullRoot() {
        // Empty YAML document with explicit null
        String yaml = "---\n";
        assertThrows(RulesetParseException.class, () -> parser.parse(yaml));
    }

    @Test
    void parseShouldThrowOnEmptyInput() {
        // Empty string produces null from Jackson readTree
        String yaml = "";
        assertThrows(RulesetParseException.class, () -> parser.parse(yaml));
    }

    @Test
    void parseShouldThrowOnNonObjectRule() {
        String yaml = """
                rules:
                  bad-rule: "not an object"
                """;

        assertThrows(RulesetParseException.class, () -> parser.parse(yaml));
    }

    @Test
    void parseShouldThrowOnNonObjectAction() {
        String yaml = """
                rules:
                  test-rule:
                    given: "$"
                    then:
                      - "not an object"
                """;

        assertThrows(RulesetParseException.class, () -> parser.parse(yaml));
    }

    @Test
    void parseShouldHandleExtendsAsString() {
        String yaml = """
                extends: "spectral:oas"
                rules: {}
                """;

        Ruleset ruleset = parser.parse(yaml);
        assertEquals(List.of("spectral:oas"), ruleset.extendsRefs());
    }

    @Test
    void parseShouldHandleExtendsAsTupleArray() {
        String yaml = """
                extends:
                  - ["spectral:oas", "off"]
                  - ["spectral:asyncapi", "warn"]
                rules: {}
                """;

        Ruleset ruleset = parser.parse(yaml);
        assertEquals(List.of("spectral:oas", "spectral:asyncapi"), ruleset.extendsRefs());
    }

    @Test
    void parseShouldHandleComplexAliasFormat() {
        String yaml = """
                aliases:
                  PathItem:
                    targets:
                      - given: "$.paths[*]"
                rules: {}
                """;

        Ruleset ruleset = parser.parse(yaml);
        assertEquals(Map.of("PathItem", "$.paths[*]"), ruleset.aliases());
    }

    @Test
    void parseShouldHandleComplexAliasWithArrayGiven() {
        String yaml = """
                aliases:
                  MultiPath:
                    targets:
                      - given:
                          - "$.paths[*]"
                          - "$.webhooks[*]"
                rules: {}
                """;

        Ruleset ruleset = parser.parse(yaml);
        assertEquals(Map.of("MultiPath", "$.paths[*]"), ruleset.aliases());
    }

    @Test
    void parseShouldHandleMissingGiven() {
        String yaml = """
                rules:
                  test-rule:
                    then:
                      function: truthy
                """;

        Ruleset ruleset = parser.parse(yaml);
        assertTrue(ruleset.rules().get("test-rule").given().isEmpty());
    }

    @Test
    void parseShouldHandleMissingThen() {
        String yaml = """
                rules:
                  test-rule:
                    given: "$"
                """;

        Ruleset ruleset = parser.parse(yaml);
        assertTrue(ruleset.rules().get("test-rule").then().isEmpty());
    }

    @Test
    void parseShouldHandleDefaultSeverity() {
        String yaml = """
                rules:
                  test-rule:
                    given: "$"
                    then:
                      function: truthy
                """;

        Ruleset ruleset = parser.parse(yaml);
        assertEquals("warn", ruleset.rules().get("test-rule").severity());
    }

    @Test
    void parseShouldHandleRuleWithFormats() {
        String yaml = """
                rules:
                  test-rule:
                    formats:
                      - oas3
                      - oas3.1
                    given: "$"
                    then:
                      function: truthy
                """;

        Ruleset ruleset = parser.parse(yaml);
        assertEquals(List.of("oas3", "oas3.1"), ruleset.rules().get("test-rule").formats());
    }

    @Test
    void parseShouldHandleRuleWithoutFormats() {
        String yaml = """
                rules:
                  test-rule:
                    given: "$"
                    then:
                      function: truthy
                """;

        Ruleset ruleset = parser.parse(yaml);
        assertNull(ruleset.rules().get("test-rule").formats());
    }

    @Test
    void parseShouldHandleActionWithNoFunctionOptions() {
        String yaml = """
                rules:
                  test-rule:
                    given: "$"
                    then:
                      function: truthy
                """;

        Ruleset ruleset = parser.parse(yaml);
        assertTrue(ruleset.rules().get("test-rule").then().get(0).functionOptions().isEmpty());
    }

    @Test
    void parseShouldHandleActionWithNoFunction() {
        String yaml = """
                rules:
                  test-rule:
                    given: "$"
                    then:
                      field: title
                """;

        Ruleset ruleset = parser.parse(yaml);
        RuleAction action = ruleset.rules().get("test-rule").then().get(0);
        assertNull(action.functionName());
        assertEquals("title", action.field());
    }

    @Test
    void parseShouldHandleFileInput() throws IOException {
        String yaml = """
                rules:
                  file-test:
                    given: "$"
                    then:
                      function: defined
                """;

        Path tempFile = Files.createTempFile("ruleset", ".yml");
        try {
            Files.writeString(tempFile, yaml);
            Ruleset ruleset = parser.parse(tempFile);
            assertEquals(1, ruleset.rules().size());
            assertEquals("defined", ruleset.rules().get("file-test").then().get(0).functionName());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void parseShouldThrowOnInvalidFilePath() {
        Path nonExistent = Path.of("/non/existent/path.yml");
        assertThrows(java.io.UncheckedIOException.class, () -> parser.parse(nonExistent));
    }

    @Test
    void parseShouldThrowOnEmptyFile() throws IOException {
        Path tempFile = Files.createTempFile("empty-ruleset", ".yml");
        try {
            Files.writeString(tempFile, "");
            assertThrows(RulesetParseException.class, () -> parser.parse(tempFile));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void parseShouldHandleMissingRulesSection() {
        String yaml = """
                extends: spectral:oas
                """;

        Ruleset ruleset = parser.parse(yaml);
        assertTrue(ruleset.rules().isEmpty());
    }

    @Test
    void parseShouldHandleNullAliases() {
        String yaml = """
                aliases: null
                rules: {}
                """;

        Ruleset ruleset = parser.parse(yaml);
        assertTrue(ruleset.aliases().isEmpty());
    }

    @Test
    void parseShouldHandleRuleDocumentationUrl() {
        String yaml = """
                rules:
                  test-rule:
                    documentationUrl: https://example.com/rules/test
                    given: "$"
                    then:
                      function: truthy
                """;

        Ruleset ruleset = parser.parse(yaml);
        assertEquals("https://example.com/rules/test", ruleset.rules().get("test-rule").documentationUrl());
    }

    @Test
    void parseShouldHandleEmptyExtends() {
        String yaml = """
                extends: []
                rules: {}
                """;

        Ruleset ruleset = parser.parse(yaml);
        assertTrue(ruleset.extendsRefs().isEmpty());
    }

    @Test
    void parseShouldHandleScalarArray() {
        String yaml = """
                rules: null
                """;

        Ruleset ruleset = parser.parse(yaml);
        assertTrue(ruleset.rules().isEmpty());
    }
}
