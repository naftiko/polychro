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

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RulesetParserEdgeCasesTest {

    private final RulesetParser parser = new RulesetParser();

    @Test
    void parseShouldIgnoreNonTextualExtendsArrayItems() {
        String yaml = """
                extends:
                  - spectral:oas
                  - 42
                  - [too, many, items]
                rules: {}
                """;
        Ruleset ruleset = parser.parse(yaml);
        assertEquals(List.of("spectral:oas"), ruleset.extendsRefs());
    }

    @Test
    void parseShouldHandleExtendsAsNullNode() {
        String yaml = """
                extends: null
                rules: {}
                """;
        Ruleset ruleset = parser.parse(yaml);
        assertTrue(ruleset.extendsRefs().isEmpty());
    }

    @Test
    void parseShouldHandleAliasWithEmptyTargets() {
        String yaml = """
                aliases:
                  Empty:
                    targets: []
                rules: {}
                """;
        Ruleset ruleset = parser.parse(yaml);
        assertTrue(ruleset.aliases().isEmpty());
    }

    @Test
    void parseShouldHandleAliasWithTargetMissingGiven() {
        String yaml = """
                aliases:
                  NoGiven:
                    targets:
                      - formats:
                          - oas3
                rules: {}
                """;
        Ruleset ruleset = parser.parse(yaml);
        assertTrue(ruleset.aliases().isEmpty());
    }

    @Test
    void parseShouldHandleAliasWithNonObjectNonTextualValue() {
        String yaml = """
                aliases:
                  WeirdAlias:
                    - not-a-valid-format
                rules: {}
                """;
        Ruleset ruleset = parser.parse(yaml);
        // Array value for alias — neither textual nor has targets — should be skipped
        assertTrue(ruleset.aliases().isEmpty());
    }

    @Test
    void parseShouldHandleGivenAsNumberNode() {
        String yaml = """
                rules:
                  test-rule:
                    given: 42
                    then:
                      function: truthy
                """;
        Ruleset ruleset = parser.parse(yaml);
        // Number node isn't textual or array — falls to default empty list
        assertTrue(ruleset.rules().get("test-rule").given().isEmpty());
    }

    @Test
    void parseShouldHandleThenAsScalar() {
        String yaml = """
                rules:
                  test-rule:
                    given: "$"
                    then: "invalid-scalar"
                """;
        Ruleset ruleset = parser.parse(yaml);
        // Scalar then — not object or array — returns empty
        assertTrue(ruleset.rules().get("test-rule").then().isEmpty());
    }

    @Test
    void parseShouldHandleFormatsNodeAsNonArray() {
        String yaml = """
                formats: "not-an-array"
                rules: {}
                """;
        Ruleset ruleset = parser.parse(yaml);
        assertTrue(ruleset.formats().isEmpty());
    }

    @Test
    void parseShouldHandleFunctionsAsNonArray() {
        String yaml = """
                functions: "not-an-array"
                rules: {}
                """;
        Ruleset ruleset = parser.parse(yaml);
        assertTrue(ruleset.functions().isEmpty());
    }

    @Test
    void parseShouldHandleExtendsNonTextualNonArraySingleTupleItem() {
        // A single-element array in extends (not a 2-element tuple)
        String yaml = """
                extends:
                  - [single]
                rules: {}
                """;
        Ruleset ruleset = parser.parse(yaml);
        // Single-element array != 2-element tuple → skipped
        assertTrue(ruleset.extendsRefs().isEmpty());
    }

    @Test
    void parseShouldHandleExtendsAsIntegerNode() {
        String yaml = """
                extends: 42
                rules: {}
                """;
        Ruleset ruleset = parser.parse(yaml);
        // Integer node is not textual or array
        assertTrue(ruleset.extendsRefs().isEmpty());
    }

    @Test
    void parseShouldHandleAliasWithNonArrayTargets() {
        String yaml = """
                aliases:
                  BadTargets:
                    targets: "not-array"
                rules: {}
                """;
        Ruleset ruleset = parser.parse(yaml);
        assertTrue(ruleset.aliases().isEmpty());
    }

    @Test
    void parseShouldHandleAliasTargetWithNonTextualNonArrayGiven() {
        String yaml = """
                aliases:
                  NumericGiven:
                    targets:
                      - given: 42
                rules: {}
                """;
        Ruleset ruleset = parser.parse(yaml);
        assertTrue(ruleset.aliases().isEmpty());
    }

    @Test
    void parseShouldHandleAliasTargetWithEmptyArrayGiven() {
        String yaml = """
                aliases:
                  EmptyGiven:
                    targets:
                      - given: []
                rules: {}
                """;
        Ruleset ruleset = parser.parse(yaml);
        assertTrue(ruleset.aliases().isEmpty());
    }

    @Test
    void parseShouldHandleNullGivenNode() {
        String yaml = """
                rules:
                  test-rule:
                    given: null
                    then:
                      function: truthy
                """;
        Ruleset ruleset = parser.parse(yaml);
        assertTrue(ruleset.rules().get("test-rule").given().isEmpty());
    }

    @Test
    void parseShouldHandleNullThenNode() {
        String yaml = """
                rules:
                  test-rule:
                    given: "$"
                    then: null
                """;
        Ruleset ruleset = parser.parse(yaml);
        assertTrue(ruleset.rules().get("test-rule").then().isEmpty());
    }

    @Test
    void parseShouldHandleNullFunctionOptionsNode() {
        String yaml = """
                rules:
                  test-rule:
                    given: "$"
                    then:
                      function: truthy
                      functionOptions: null
                """;
        Ruleset ruleset = parser.parse(yaml);
        assertTrue(ruleset.rules().get("test-rule").then().get(0).functionOptions().isEmpty());
    }

    @Test
    void parseShouldHandleNonObjectFunctionOptions() {
        String yaml = """
                rules:
                  test-rule:
                    given: "$"
                    then:
                      function: truthy
                      functionOptions: "string-value"
                """;
        Ruleset ruleset = parser.parse(yaml);
        assertTrue(ruleset.rules().get("test-rule").then().get(0).functionOptions().isEmpty());
    }

    @Test
    void parseShouldHandleNonTextualFunctionsItems() {
        String yaml = """
                functions:
                  - myFunc
                  - 42
                rules: {}
                """;
        Ruleset ruleset = parser.parse(yaml);
        // Only textual items are included
        assertEquals(List.of("myFunc"), ruleset.functions());
    }

    @Test
    void parseShouldHandleAliasObjectWithoutTargetsKey() {
        String yaml = """
                aliases:
                  NoTargets:
                    something: else
                rules: {}
                """;
        Ruleset ruleset = parser.parse(yaml);
        assertTrue(ruleset.aliases().isEmpty());
    }

    @Test
    void parseShouldThrowWhenRootIsScalar() {
        String yaml = "\"just a string\"";
        assertThrows(RulesetParseException.class, () -> parser.parse(yaml));
    }

    @Test
    void parseShouldHandleRuleWithNullMessage() {
        String yaml = """
                rules:
                  test-rule:
                    message: null
                    given: "$"
                    then:
                      function: truthy
                """;
        Ruleset ruleset = parser.parse(yaml);
        assertNull(ruleset.rules().get("test-rule").message());
    }

    @Test
    void parseShouldHandleRuleWithNullDescription() {
        String yaml = """
                rules:
                  test-rule:
                    description: null
                    given: "$"
                    then:
                      function: truthy
                """;
        Ruleset ruleset = parser.parse(yaml);
        assertNull(ruleset.rules().get("test-rule").description());
    }

    @Test
    void overridesWithNullNodeShouldReturnEmptyList() {
        String yaml = """
                rules:
                  test-rule:
                    message: "Test"
                    severity: warn
                    recommended: true
                    given: "$"
                    then:
                      function: truthy
                """;
        Ruleset ruleset = parser.parse(yaml);
        assertEquals(List.of(), ruleset.overrides());
    }

    @Test
    void overridesWithNonObjectItemShouldBeSkipped() {
        String yaml = """
                rules:
                  test-rule:
                    message: "Test"
                    severity: warn
                    recommended: true
                    given: "$"
                    then:
                      function: truthy
                overrides:
                  - files:
                      - "**/*.json"
                    rules:
                      test-rule:
                        severity: error
                        recommended: true
                        given: "$"
                        then:
                          function: truthy
                  - "not an object"
                """;
        Ruleset ruleset = parser.parse(yaml);
        assertEquals(1, ruleset.overrides().size());
    }

    @Test
    void severityBooleanFalseShouldBeInterpretedAsOff() {
        // YAML 'off' without quotes is boolean false
        String yaml = """
                rules:
                  test-rule:
                    message: "Test"
                    severity: off
                    recommended: true
                    given: "$"
                    then:
                      function: truthy
                """;
        Ruleset ruleset = parser.parse(yaml);
        assertEquals("off", ruleset.rules().get("test-rule").severity());
    }

    @Test
    void severityBooleanTrueShouldBeInterpretedAsString() {
        // YAML 'on' or 'true' should just give the text representation
        String yaml = """
                rules:
                  test-rule:
                    message: "Test"
                    severity: true
                    recommended: true
                    given: "$"
                    then:
                      function: truthy
                """;
        Ruleset ruleset = parser.parse(yaml);
        assertEquals("true", ruleset.rules().get("test-rule").severity());
    }

    @Test
    void nullSeverityNodeShouldDefaultToWarn() {
        String yaml = """
                rules:
                  test-rule:
                    message: "Test"
                    recommended: true
                    given: "$"
                    then:
                      function: truthy
                """;
        Ruleset ruleset = parser.parse(yaml);
        assertEquals("warn", ruleset.rules().get("test-rule").severity());
    }

    @Test
    void overridesAsScalarShouldReturnEmptyList() {
        String yaml = """
                rules:
                  test-rule:
                    message: "Test"
                    severity: warn
                    recommended: true
                    given: "$"
                    then:
                      function: truthy
                overrides: "not-an-array"
                """;
        Ruleset ruleset = parser.parse(yaml);
        assertEquals(List.of(), ruleset.overrides());
    }

    @Test
    void nullYamlSeverityShouldDefaultToWarn() {
        // Explicit YAML null value
        String yaml = """
                rules:
                  test-rule:
                    message: "Test"
                    severity: ~
                    recommended: true
                    given: "$"
                    then:
                      function: truthy
                """;
        Ruleset ruleset = parser.parse(yaml);
        assertEquals("warn", ruleset.rules().get("test-rule").severity());
    }
}
