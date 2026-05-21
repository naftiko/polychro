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

import io.polychro.spi.Diagnostic;
import io.polychro.spi.Document;
import io.polychro.spi.Severity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RulesetValidatorTest {

    private static RulesetValidator newValidator() {
        Ruleset ruleset = new Ruleset(null, null, null, null, null, null, Map.of(), null);
        return new RulesetValidator(ruleset, false);
    }

    @Test
    void nameShouldReturnRuleset() {
        RulesetValidator validator = newValidator();
        assertEquals("ruleset", validator.name());
    }

    @Test
    void validateShouldReturnEmptyForEmptyRuleset() {
        Ruleset ruleset = new Ruleset(null, null, null, null, null, null, Map.of(), null);
        RulesetValidator validator = new RulesetValidator(ruleset, false);
        Document doc = Document.fromString("{\"info\": {\"name\": \"test\"}}", "json");

        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.isEmpty());
    }

    @Test
    void validateShouldProduceDiagnosticsForViolation() {
        Rule rule = new Rule("name-truthy", "Name must not be empty", null, "warn", true,
                null, null, List.of("$.info.name"),
                List.of(new RuleAction(null, "truthy", Map.of())));
        Ruleset ruleset = new Ruleset(null, null, null, null, null, null, Map.of("name-truthy", rule), null);
        RulesetValidator validator = new RulesetValidator(ruleset, false);
        Document doc = Document.fromString("{\"info\": {\"name\": \"\"}}", "json");

        List<Diagnostic> results = validator.validate(doc);
        assertEquals(1, results.size());
        assertEquals("Name must not be empty", results.get(0).message());
        assertEquals(Severity.WARN, results.get(0).severity());
    }

    @Test
    void validateShouldExecuteMultipleRules() {
        Rule rule1 = new Rule("name-truthy", "Name required", null, "error", true,
                null, null, List.of("$.info.name"),
                List.of(new RuleAction(null, "truthy", Map.of())));
        Rule rule2 = new Rule("desc-truthy", "Description required", null, "warn", true,
                null, null, List.of("$.info"),
                List.of(new RuleAction("description", "truthy", Map.of())));
        Ruleset ruleset = new Ruleset(null, null, null, null, null, null,
                Map.of("name-truthy", rule1, "desc-truthy", rule2), null);
        RulesetValidator validator = new RulesetValidator(ruleset, false);
        Document doc = Document.fromString("{\"info\": {\"name\": \"\"}}", "json");

        List<Diagnostic> results = validator.validate(doc);
        assertEquals(2, results.size());
        // Sorted by severity: ERROR first
        assertEquals(Severity.ERROR, results.get(0).severity());
        assertEquals(Severity.WARN, results.get(1).severity());
    }

    @Test
    void validateShouldSkipNonRecommendedRulesWhenNotIncluded() {
        Rule rule = new Rule("optional-rule", "Optional", null, "warn", false,
                null, null, List.of("$.info.name"),
                List.of(new RuleAction(null, "truthy", Map.of())));
        Ruleset ruleset = new Ruleset(null, null, null, null, null, null, Map.of("optional-rule", rule), null);
        RulesetValidator validator = new RulesetValidator(ruleset, false);
        Document doc = Document.fromString("{\"info\": {\"name\": \"\"}}", "json");

        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.isEmpty());
    }

    @Test
    void validateShouldIncludeNonRecommendedRulesWhenEnabled() {
        Rule rule = new Rule("optional-rule", "Optional", null, "warn", false,
                null, null, List.of("$.info.name"),
                List.of(new RuleAction(null, "truthy", Map.of())));
        Ruleset ruleset = new Ruleset(null, null, null, null, null, null, Map.of("optional-rule", rule), null);
        RulesetValidator validator = new RulesetValidator(ruleset, true);
        Document doc = Document.fromString("{\"info\": {\"name\": \"\"}}", "json");

        List<Diagnostic> results = validator.validate(doc);
        assertEquals(1, results.size());
    }

    @Test
    void validateShouldSkipOffRules() {
        Rule rule = new Rule("disabled-rule", "Disabled", null, "off", true,
                null, null, List.of("$.info.name"),
                List.of(new RuleAction(null, "truthy", Map.of())));
        Ruleset ruleset = new Ruleset(null, null, null, null, null, null, Map.of("disabled-rule", rule), null);
        RulesetValidator validator = new RulesetValidator(ruleset, false);
        Document doc = Document.fromString("{\"info\": {\"name\": \"\"}}", "json");

        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.isEmpty());
    }

    @Test
    void validateShouldSortResultsBySeverityThenPath() {
        Rule rule1 = new Rule("warn-rule", "Warn issue", null, "warn", true,
                null, null, List.of("$.b"),
                List.of(new RuleAction(null, "truthy", Map.of())));
        Rule rule2 = new Rule("error-rule", "Error issue", null, "error", true,
                null, null, List.of("$.a"),
                List.of(new RuleAction(null, "truthy", Map.of())));
        Ruleset ruleset = new Ruleset(null, null, null, null, null, null,
                Map.of("warn-rule", rule1, "error-rule", rule2), null);
        RulesetValidator validator = new RulesetValidator(ruleset, false);
        Document doc = Document.fromString("{\"a\": \"\", \"b\": \"\"}", "json");

        List<Diagnostic> results = validator.validate(doc);
        assertEquals(2, results.size());
        assertEquals(Severity.ERROR, results.get(0).severity());
        assertEquals(Severity.WARN, results.get(1).severity());
    }

    @Test
    void ruleWithEmptyGivenAndAliasesShouldNotFail() {
        Rule rule = new Rule("empty-given", "No path", null, "warn", true,
                null, null, List.of(),
                List.of(new RuleAction(null, "truthy", Map.of())));
        Ruleset ruleset = new Ruleset(null, Map.of("Info", "$.info"), null, null, null, null,
                Map.of("empty-given", rule), null);
        RulesetValidator validator = new RulesetValidator(ruleset, false);
        Document doc = Document.fromString("{\"info\": {\"name\": \"test\"}}", "json");

        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.isEmpty());
    }

    @Test
    void validateShouldSkipRulesWhenFormatsDoNotMatchDocumentFormat() {
        Rule rule = new Rule("markdown-only", "Markdown only", null, "warn", true,
                List.of("markdown"), null, List.of("$.info.name"),
                List.of(new RuleAction(null, "truthy", Map.of())));
        Ruleset ruleset = new Ruleset(null, null, null, null, null, null, Map.of("markdown-only", rule), null);
        RulesetValidator validator = new RulesetValidator(ruleset, false);
        Document doc = Document.fromString("{\"info\": {\"name\": \"\"}}", "json");

        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.isEmpty());
    }

    @Test
    void validateShouldRunRulesWhenFormatsMatchDocumentFormatAlias() {
        Rule rule = new Rule("yaml-only", "YAML only", null, "warn", true,
                List.of("yml"), null, List.of("$.info.name"),
                List.of(new RuleAction(null, "truthy", Map.of())));
        Ruleset ruleset = new Ruleset(null, null, null, null, null, null, Map.of("yaml-only", rule), null);
        RulesetValidator validator = new RulesetValidator(ruleset, false);
        Document doc = Document.fromString("info:\n  name: \"\"\n", "yaml");

        List<Diagnostic> results = validator.validate(doc);
        assertEquals(1, results.size());
        assertEquals("YAML only", results.get(0).message());
    }

    @Test
    void validateShouldRunRulesWhenRuleFormatsAreEmpty() {
        // A rule with an explicit but empty formats list applies to every document
        // (this exercises the early-return branch in the format filter).
        Rule rule = new Rule("any-format", "Any format", null, "warn", true,
                List.of(), null, List.of("$.info.name"),
                List.of(new RuleAction(null, "truthy", Map.of())));
        Ruleset ruleset = new Ruleset(null, null, null, null, null, null, Map.of("any-format", rule), null);
        RulesetValidator validator = new RulesetValidator(ruleset, false);
        Document doc = Document.fromString("{\"info\": {\"name\": \"\"}}", "json");

        List<Diagnostic> results = validator.validate(doc);
        assertEquals(1, results.size());
        assertEquals("Any format", results.get(0).message());
    }

    @Test
    void validateShouldSkipRulesWhenDocumentFormatIsBlank() {
        // Blank format strings normalize to null inside the Document constructor;
        // a rule that restricts to a concrete format must therefore be skipped.
        Rule rule = new Rule("json-only", "JSON only", null, "warn", true,
                List.of("json"), null, List.of("$.info.name"),
                List.of(new RuleAction(null, "truthy", Map.of())));
        Ruleset ruleset = new Ruleset(null, null, null, null, null, null, Map.of("json-only", rule), null);
        RulesetValidator validator = new RulesetValidator(ruleset, false);
        Document doc = new Document(com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode(),
                "   ", null);

        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.isEmpty());
    }

    @Test
    void validateShouldSkipRulesWhenDocumentFormatIsNull() {
        Rule rule = new Rule("json-only", "JSON only", null, "warn", true,
                List.of("json"), null, List.of("$.info.name"),
                List.of(new RuleAction(null, "truthy", Map.of())));
        Ruleset ruleset = new Ruleset(null, null, null, null, null, null, Map.of("json-only", rule), null);
        RulesetValidator validator = new RulesetValidator(ruleset, false);
        Document doc = new Document(com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode(),
                (String) null, null);

        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.isEmpty());
    }

    @Test
    void validateShouldRunRulesWhenRuleFormatUsesHtmlAlias() {
        // The "htm" alias must normalise to "html" so a rule restricted to "htm"
        // still fires against an html-formatted document. We use the `falsy`
        // builtin against the (non-blank) projected root so the rule produces
        // exactly one diagnostic when format filtering admits it.
        Rule rule = new Rule("htm-only", "HTML only", null, "warn", true,
                List.of("htm"), null, List.of("$"),
                List.of(new RuleAction(null, "falsy", Map.of())));
        Ruleset ruleset = new Ruleset(null, null, null, null, null, null, Map.of("htm-only", rule), null);
        RulesetValidator validator = new RulesetValidator(ruleset, false);
        Document doc = Document.fromString("<html><body>hi</body></html>", "html");

        List<Diagnostic> results = validator.validate(doc);
        assertEquals(1, results.size(), "Rule must have fired to produce its diagnostic");
        assertEquals("HTML only", results.get(0).message());
    }
}
