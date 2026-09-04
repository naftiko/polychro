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
import io.polychro.spi.SourceRange;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RulesetValidatorTest {

    private static RulesetValidator newValidator() {
        Ruleset ruleset = new Ruleset(null, null, null, null, null, Map.of(), null);
        return new RulesetValidator(ruleset, false);
    }

    @Test
    void nameShouldReturnRuleset() {
        RulesetValidator validator = newValidator();
        assertEquals("ruleset", validator.name());
    }

    @Test
    void validateShouldReturnEmptyForEmptyRuleset() {
        Ruleset ruleset = new Ruleset(null, null, null, null, null, Map.of(), null);
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
        Ruleset ruleset = new Ruleset(null, null, null, null, null, Map.of("name-truthy", rule), null);
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
        Ruleset ruleset = new Ruleset(null, null, null, null, null,
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
        Ruleset ruleset = new Ruleset(null, null, null, null, null, Map.of("optional-rule", rule), null);
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
        Ruleset ruleset = new Ruleset(null, null, null, null, null, Map.of("optional-rule", rule), null);
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
        Ruleset ruleset = new Ruleset(null, null, null, null, null, Map.of("disabled-rule", rule), null);
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
        Ruleset ruleset = new Ruleset(null, null, null, null, null,
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
        Ruleset ruleset = new Ruleset(null, Map.of("Info", "$.info"), null, null, null,
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
        Ruleset ruleset = new Ruleset(null, null, null, null, null, Map.of("markdown-only", rule), null);
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
        Ruleset ruleset = new Ruleset(null, null, null, null, null, Map.of("yaml-only", rule), null);
        RulesetValidator validator = new RulesetValidator(ruleset, false);
        Document doc = Document.fromString("info:\n  name: \"\"\n", "yaml");

        List<Diagnostic> results = validator.validate(doc);
        assertEquals(1, results.size());
        assertEquals("YAML only", results.get(0).message());
    }

    @Test
    void validateShouldRunRulesWhenRuleFormatsAreExplicitlyEmpty() {
        // An explicit but empty rule-level formats list is a deliberate "match nothing" —
        // distinct from an omitted (null) formats, which inherits the ruleset's formats
        // (or matches everything when the ruleset itself has none). Mirrors Spectral, which
        // preserves this null-vs-empty distinction rather than treating [] as "not declared".
        Rule rule = new Rule("no-format", "No format", null, "warn", true,
                List.of(), null, List.of("$.info.name"),
                List.of(new RuleAction(null, "truthy", Map.of())));
        Ruleset ruleset = new Ruleset(null, null, null, null, null, Map.of("no-format", rule), null);
        RulesetValidator validator = new RulesetValidator(ruleset, false);
        Document doc = Document.fromString("{\"info\": {\"name\": \"\"}}", "json");

        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.isEmpty(),
                () -> "Explicit formats: [] must match no document, got: " + results);
    }

    @Test
    void validateShouldSkipRulesWhenDocumentFormatIsBlank() {
        // Blank format strings normalize to null inside the Document constructor;
        // a rule that restricts to a concrete format must therefore be skipped.
        Rule rule = new Rule("json-only", "JSON only", null, "warn", true,
                List.of("json"), null, List.of("$.info.name"),
                List.of(new RuleAction(null, "truthy", Map.of())));
        Ruleset ruleset = new Ruleset(null, null, null, null, null, Map.of("json-only", rule), null);
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
        Ruleset ruleset = new Ruleset(null, null, null, null, null, Map.of("json-only", rule), null);
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
        Ruleset ruleset = new Ruleset(null, null, null, null, null, Map.of("htm-only", rule), null);
        RulesetValidator validator = new RulesetValidator(ruleset, false);
        Document doc = Document.fromString("<html><body>hi</body></html>", "html");

        List<Diagnostic> results = validator.validate(doc);
        assertEquals(1, results.size(), "Rule must have fired to produce its diagnostic");
        assertEquals("HTML only", results.get(0).message());
    }

    // --- Issue #32: ruleset diagnostics must carry source ranges (built-in functions) ---

    @Test
    void validateShouldPopulateSourceRangeForBuiltinViolationOnYaml() {
        // Regression guard for #32: a `pattern` violation on a YAML document must
        // produce a Diagnostic whose `range` points at the offending scalar, the way
        // Spectral does — not range == null. The baseUri sits on line 3.
        Rule rule = new Rule("baseuri-no-trailing-slash", "baseUri must not end with /",
                null, "warn", true, null, null, List.of("$.consumes[*].baseUri"),
                List.of(new RuleAction(null, "pattern", Map.of("notMatch", "/$"))));
        Ruleset ruleset = new Ruleset(null, null, null, null, null,
                Map.of("baseuri-no-trailing-slash", rule), null);
        RulesetValidator validator = new RulesetValidator(ruleset, false);
        String yaml = "consumes:\n  - name: api\n    baseUri: \"https://example.com/\"\n";
        Document doc = Document.fromString(yaml, "yaml");

        List<Diagnostic> results = validator.validate(doc);
        assertEquals(1, results.size());
        assertNotNull(results.get(0).range(),
                "#32: ruleset diagnostics must carry a SourceRange, not null");
        assertEquals(2, results.get(0).range().startLine(),
                "#32: range must point at the baseUri scalar (human line 3, 0-based 2)");
    }

    @Test
    void validateShouldSetConcretePathForBuiltinViolation() {
        // #32: the diagnostic path must be the concrete matched location
        // ($.consumes[0].baseUri), not the rule's `given` selector pattern
        // ($.consumes[*].baseUri), so path and range stay consistent.
        Rule rule = new Rule("baseuri-no-trailing-slash", "baseUri must not end with /",
                null, "warn", true, null, null, List.of("$.consumes[*].baseUri"),
                List.of(new RuleAction(null, "pattern", Map.of("notMatch", "/$"))));
        Ruleset ruleset = new Ruleset(null, null, null, null, null,
                Map.of("baseuri-no-trailing-slash", rule), null);
        RulesetValidator validator = new RulesetValidator(ruleset, false);
        String yaml = "consumes:\n  - name: api\n    baseUri: \"https://example.com/\"\n";
        Document doc = Document.fromString(yaml, "yaml");

        List<Diagnostic> results = validator.validate(doc);
        assertEquals(1, results.size());
        assertEquals("$.consumes[0].baseUri", results.get(0).path(),
                "#32: path must be the concrete match, not the [*] selector");
    }

    @Test
    void validateShouldResolveSourceRangeWhenKeyContainsDot() {
        // #32 edge case: a key that itself contains a dot (e.g. "x-meta.owner") is keyed as
        // $.x-meta.owner by BOTH JacksonSourceMap and JsonPathEvaluator.toDotNotation, so the two
        // path strings COINCIDE and the lookup resolves. This proves the round-trip is consistent
        // for an isolated dotted key — it does NOT prove the path is unambiguous: $.x-meta.owner is
        // indistinguishable from a nested x-meta: { owner: ... } (see the collision test below and
        // the JacksonSourceMap / toDotNotation Javadocs). Here there is no nesting, so resolution
        // is exact and the range is not silently lost.
        Rule rule = new Rule("owner-required", "owner must be present",
                null, "warn", true, null, null, List.of("$['x-meta.owner']"),
                List.of(new RuleAction(null, "truthy", null)));
        Ruleset ruleset = new Ruleset(null, null, null, null, null,
                Map.of("owner-required", rule), null);
        RulesetValidator validator = new RulesetValidator(ruleset, false);
        String yaml = "name: api\n\"x-meta.owner\": \"\"\n";
        Document doc = Document.fromString(yaml, "yaml");

        List<Diagnostic> results = validator.validate(doc);
        assertEquals(1, results.size());
        assertNotNull(results.get(0).range(),
                "#32: a dotted key must still resolve to a SourceRange, not null");
        assertEquals(1, results.get(0).range().startLine(),
                "#32: range must point at the dotted-key scalar (human line 2, 0-based 1)");
    }

    @Test
    void validateShouldKeepFirstLocationWhenDottedKeyCollidesWithNestedPath() {
        // #32 ambiguity guard: a flat key "x-meta.owner" and a nested structure x-meta: { owner }
        // BOTH map to the path $.x-meta.owner. The source map keeps the FIRST token encountered
        // (putIfAbsent), so resolution is documented-but-imprecise rather than throwing or
        // silently returning null. This test pins that behaviour so a future change to the keying
        // (or to putIfAbsent -> put) is caught.
        Rule rule = new Rule("owner-required", "owner must be present",
                null, "warn", true, null, null, List.of("$['x-meta.owner']"),
                List.of(new RuleAction(null, "truthy", null)));
        Ruleset ruleset = new Ruleset(null, null, null, null, null,
                Map.of("owner-required", rule), null);
        RulesetValidator validator = new RulesetValidator(ruleset, false);
        // The flat dotted key appears on line 2, the colliding nested "owner" on line 4.
        String yaml = "name: api\n\"x-meta.owner\": \"\"\nx-meta:\n  owner: present\n";
        Document doc = Document.fromString(yaml, "yaml");

        List<Diagnostic> results = validator.validate(doc);
        assertEquals(1, results.size());
        assertNotNull(results.get(0).range(),
                "#32: a colliding dotted path still resolves to a SourceRange, never null");
        assertEquals(1, results.get(0).range().startLine(),
                "#32: on a $.x-meta.owner collision the FIRST token (flat key, human line 2, 0-based 1) wins");
    }

    @Test
    void validateShouldPropagateEndOfRangeToDiagnosticOnYaml() {
        // #32 end propagation (HIGH golden): the Diagnostic range must not collapse to a
        // single point. For a quoted scalar, Spectral spans the whole value INCLUDING both
        // quotes, so on a mono-line scalar the end sits strictly after the start on the same
        // line. JacksonSourceMap.toRange() now spans the quoted scalar end-exclusive.
        Rule rule = new Rule("baseuri-no-trailing-slash", "baseUri must not end with /",
                null, "warn", true, null, null, List.of("$.consumes[*].baseUri"),
                List.of(new RuleAction(null, "pattern", Map.of("notMatch", "/$"))));
        Ruleset ruleset = new Ruleset(null, null, null, null, null,
                Map.of("baseuri-no-trailing-slash", rule), null);
        RulesetValidator validator = new RulesetValidator(ruleset, false);
        String yaml = "consumes:\n  - name: api\n    baseUri: \"https://example.com/\"\n";
        Document doc = Document.fromString(yaml, "yaml");

        List<Diagnostic> results = validator.validate(doc);
        assertEquals(1, results.size());
        SourceRange range = results.get(0).range();
        assertNotNull(range, "#32: ruleset diagnostics must carry a SourceRange, not null");
        assertEquals(range.startLine(), range.endLine(),
                "#32: a mono-line scalar must stay on a single line");
        assertTrue(range.endColumn() > range.startColumn(),
                "#32: the range must span the scalar (quotes included), so end must follow start");
    }

    // --- Issue #83: spec-level format (oas2/oas3/aas2/aas3) filtering ---

    @Test
    void validateShouldRunRulesScopedToOas3ForOpenapiThreeDocument() {
        Rule rule = new Rule("oas3-only", "OAS3 only", null, "warn", true,
                List.of("oas3"), null, List.of("$.info.name"),
                List.of(new RuleAction(null, "truthy", Map.of())));
        Ruleset ruleset = new Ruleset(null, null, null, null, null, Map.of("oas3-only", rule), null);
        RulesetValidator validator = new RulesetValidator(ruleset, false);
        Document doc = Document.fromString("openapi: 3.0.2\ninfo:\n  name: \"\"\n", "yaml");

        List<Diagnostic> results = validator.validate(doc);
        assertEquals(1, results.size());
        assertEquals("OAS3 only", results.get(0).message());
    }

    @Test
    void validateShouldSkipRulesScopedToOas2ForOpenapiThreeDocument() {
        Rule rule = new Rule("oas2-only", "OAS2 only", null, "warn", true,
                List.of("oas2"), null, List.of("$.info.name"),
                List.of(new RuleAction(null, "truthy", Map.of())));
        Ruleset ruleset = new Ruleset(null, null, null, null, null, Map.of("oas2-only", rule), null);
        RulesetValidator validator = new RulesetValidator(ruleset, false);
        Document doc = Document.fromString("openapi: 3.0.2\ninfo:\n  name: \"\"\n", "yaml");

        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.isEmpty());
    }

    @Test
    void validateShouldRunRulesScopedToOas2ForSwaggerTwoDocument() {
        Rule rule = new Rule("oas2-only", "OAS2 only", null, "warn", true,
                List.of("oas2"), null, List.of("$.info.name"),
                List.of(new RuleAction(null, "truthy", Map.of())));
        Ruleset ruleset = new Ruleset(null, null, null, null, null, Map.of("oas2-only", rule), null);
        RulesetValidator validator = new RulesetValidator(ruleset, false);
        Document doc = Document.fromString("swagger: \"2.0\"\ninfo:\n  name: \"\"\n", "yaml");

        List<Diagnostic> results = validator.validate(doc);
        assertEquals(1, results.size());
        assertEquals("OAS2 only", results.get(0).message());
    }

    @Test
    void validateShouldSkipRulesScopedToOas3ForSwaggerTwoDocument() {
        Rule rule = new Rule("oas3-only", "OAS3 only", null, "warn", true,
                List.of("oas3"), null, List.of("$.info.name"),
                List.of(new RuleAction(null, "truthy", Map.of())));
        Ruleset ruleset = new Ruleset(null, null, null, null, null, Map.of("oas3-only", rule), null);
        RulesetValidator validator = new RulesetValidator(ruleset, false);
        Document doc = Document.fromString("swagger: \"2.0\"\ninfo:\n  name: \"\"\n", "yaml");

        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.isEmpty());
    }

    @Test
    void validateShouldRunFormatAgnosticRuleOnBothOasVersions() {
        Rule rule = new Rule("any-oas", "Any OAS", null, "warn", true,
                null, null, List.of("$.info.name"),
                List.of(new RuleAction(null, "truthy", Map.of())));
        Ruleset ruleset = new Ruleset(null, null, null, null, null, Map.of("any-oas", rule), null);
        RulesetValidator validator = new RulesetValidator(ruleset, false);

        Document oas2Doc = Document.fromString("swagger: \"2.0\"\ninfo:\n  name: \"\"\n", "yaml");
        Document oas3Doc = Document.fromString("openapi: 3.0.2\ninfo:\n  name: \"\"\n", "yaml");

        assertEquals(1, validator.validate(oas2Doc).size());
        assertEquals(1, validator.validate(oas3Doc).size());
    }

    @Test
    void validateShouldSkipSpecScopedRuleForPlainYamlDocumentWithNoSpecFormat() {
        Rule rule = new Rule("oas3-only", "OAS3 only", null, "warn", true,
                List.of("oas3"), null, List.of("$.info.name"),
                List.of(new RuleAction(null, "truthy", Map.of())));
        Ruleset ruleset = new Ruleset(null, null, null, null, null, Map.of("oas3-only", rule), null);
        RulesetValidator validator = new RulesetValidator(ruleset, false);
        Document doc = Document.fromString("info:\n  name: \"\"\n", "yaml");

        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.isEmpty());
    }

    // --- ruleset-level `formats:` gate for rules with no formats of their own ---

    @Test
    void validateShouldSkipUnscopedRuleWhenDocumentDoesNotMatchRulesetLevelFormats() {
        // A rule with no formats of its own (e.g. info-contact in polychro:openapi) must NOT
        // fire against a plain YAML/JSON document that happens to contain "info" — the
        // ruleset-level `formats:` restriction gates it, mirroring Spectral's top-level
        // spectral:oas formats restriction.
        Rule rule = new Rule("info-name-truthy", "Name required", null, "warn", true,
                null, null, List.of("$.info.name"),
                List.of(new RuleAction(null, "truthy", Map.of())));
        Ruleset ruleset = new Ruleset(null, null, null, List.of("oas3"), null,
                Map.of("info-name-truthy", rule), null);
        RulesetValidator validator = new RulesetValidator(ruleset, false);
        Document doc = Document.fromString("info:\n  name: \"\"\n", "yaml");

        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.isEmpty(),
                () -> "Unscoped rule must be gated by the ruleset-level formats, got: " + results);
    }

    @Test
    void validateShouldRunUnscopedRuleWhenDocumentMatchesRulesetLevelFormats() {
        Rule rule = new Rule("info-name-truthy", "Name required", null, "warn", true,
                null, null, List.of("$.info.name"),
                List.of(new RuleAction(null, "truthy", Map.of())));
        Ruleset ruleset = new Ruleset(null, null, null, List.of("oas3"), null,
                Map.of("info-name-truthy", rule), null);
        RulesetValidator validator = new RulesetValidator(ruleset, false);
        Document doc = Document.fromString("openapi: 3.0.2\ninfo:\n  name: \"\"\n", "yaml");

        List<Diagnostic> results = validator.validate(doc);
        assertEquals(1, results.size(),
                () -> "Unscoped rule must fire once the document matches the ruleset-level "
                        + "formats, got: " + results);
    }

    @Test
    void validateShouldPreferRuleLevelFormatsOverRulesetLevelFormats() {
        // A rule that declares its own formats overrides the ruleset-level restriction entirely —
        // here the ruleset restricts to oas3, but the rule restricts to oas2, and an oas2
        // document must still fire it.
        Rule rule = new Rule("oas2-only", "OAS2 only", null, "warn", true,
                List.of("oas2"), null, List.of("$.info.name"),
                List.of(new RuleAction(null, "truthy", Map.of())));
        Ruleset ruleset = new Ruleset(null, null, null, List.of("oas3"), null,
                Map.of("oas2-only", rule), null);
        RulesetValidator validator = new RulesetValidator(ruleset, false);
        Document doc = Document.fromString("swagger: \"2.0\"\ninfo:\n  name: \"\"\n", "yaml");

        List<Diagnostic> results = validator.validate(doc);
        assertEquals(1, results.size(),
                () -> "A rule's own formats must override the ruleset-level restriction, got: "
                        + results);
    }

    @Test
    void validateShouldRunUnscopedRuleWhenRulesetHasNoFormatsRestriction() {
        // No ruleset-level formats and no rule-level formats — applies to every document, same
        // as before this gate was introduced.
        Rule rule = new Rule("any-format", "Any format", null, "warn", true,
                null, null, List.of("$.info.name"),
                List.of(new RuleAction(null, "truthy", Map.of())));
        Ruleset ruleset = new Ruleset(null, null, null, null, null,
                Map.of("any-format", rule), null);
        RulesetValidator validator = new RulesetValidator(ruleset, false);
        Document doc = Document.fromString("info:\n  name: \"\"\n", "yaml");

        List<Diagnostic> results = validator.validate(doc);
        assertEquals(1, results.size());
    }

    @Test
    void validateShouldSkipUnscopedRuleWhenRulesetLevelFormatsIsExplicitlyEmpty() {
        // An explicit, non-null empty ruleset-level `formats: []` is a deliberate "match no
        // document" restriction — distinct from an omitted `formats:` (null), which leaves
        // unscoped rules format-agnostic. Before this fix, Ruleset's constructor collapsed
        // both null and [] to List.of(), so this explicit deny-all list was indistinguishable
        // from "no restriction" and unscoped rules ran against every document regardless.
        Rule rule = new Rule("info-name-truthy", "Name required", null, "warn", true,
                null, null, List.of("$.info.name"),
                List.of(new RuleAction(null, "truthy", Map.of())));
        Ruleset ruleset = new Ruleset(null, null, null, List.of(), null,
                Map.of("info-name-truthy", rule), null);
        RulesetValidator validator = new RulesetValidator(ruleset, false);
        Document doc = Document.fromString("info:\n  name: \"\"\n", "yaml");

        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.isEmpty(),
                () -> "An explicitly empty ruleset-level formats must match no document, got: "
                        + results);
    }
}
