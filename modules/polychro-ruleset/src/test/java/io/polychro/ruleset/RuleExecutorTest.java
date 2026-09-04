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
import io.polychro.spi.Diagnostic;
import io.polychro.spi.Document;
import io.polychro.spi.Severity;
import io.polychro.spi.SourceRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RuleExecutorTest {

    private RuleExecutor executor;
    private static final ObjectMapper JSON = new ObjectMapper();

    @BeforeEach
    void setUp() {
        executor = new RuleExecutor(new JsonPathEvaluator());
    }

    @Test
    void executeShouldReturnDiagnosticForSingleRuleSingleMatch() throws Exception {
        JsonNode root = JSON.readTree("{\"info\": {\"name\": \"\"}}");
        Rule rule = new Rule("test-rule", "Name must be truthy", null, "warn", true,
                null, null, List.of("$.info.name"), List.of(new RuleAction(null, "truthy", Map.of())));

        List<Diagnostic> results = executor.execute(rule, root);
        assertEquals(1, results.size());
        assertEquals("Name must be truthy", results.get(0).message());
        assertEquals("test-rule", results.get(0).code());
        assertEquals(Severity.WARN, results.get(0).severity());
    }

    @Test
    void executeShouldReturnMultipleDiagnosticsForMultipleMatches() throws Exception {
        JsonNode root = JSON.readTree("{\"items\": [{\"name\": \"\"}, {\"name\": \"\"}]}");
        Rule rule = new Rule("items-name", "Item name required", null, "error", true,
                null, null, List.of("$.items[*].name"), List.of(new RuleAction(null, "truthy", Map.of())));

        List<Diagnostic> results = executor.execute(rule, root);
        assertEquals(2, results.size());
        results.forEach(d -> assertEquals(Severity.ERROR, d.severity()));
    }

    @Test
    void executeShouldResolveFieldAndApplyFunction() throws Exception {
        JsonNode root = JSON.readTree("{\"info\": {\"description\": \"\"}}");
        Rule rule = new Rule("desc-required", "Description required", null, "warn", true,
                null, null, List.of("$.info"),
                List.of(new RuleAction("description", "truthy", Map.of())));

        List<Diagnostic> results = executor.execute(rule, root);
        assertEquals(1, results.size());
    }

    @Test
    void executeShouldProduceDiagnosticWhenFieldIsMissing() throws Exception {
        JsonNode root = JSON.readTree("{\"info\": {\"name\": \"test\"}}");
        Rule rule = new Rule("tags-required", "Tags required", null, "warn", true,
                null, null, List.of("$.info"),
                List.of(new RuleAction("tags", "truthy", Map.of())));

        List<Diagnostic> results = executor.execute(rule, root);
        assertEquals(1, results.size());
    }

    // --- diagnostic path must only descend into `field` when it actually exists on the
    // matched node, mirroring Spectral (verified against the real Spectral CLI: a
    // present-but-falsy field descends into the path, an entirely absent field does not) ---

    @Test
    void executeShouldReportMatchedNodePathWhenFieldIsEntirelyAbsent() throws Exception {
        Document doc = Document.fromString("info:\n  name: test\n", "yaml");
        Rule rule = new Rule("tags-required", "Tags required", null, "warn", true,
                null, null, List.of("$.info"),
                List.of(new RuleAction("tags", "truthy", Map.of())));

        List<Diagnostic> results = executor.execute(rule, doc);
        assertEquals(1, results.size());
        assertEquals("$.info", results.get(0).path(),
                "an entirely absent field must not be appended to the diagnostic path");
    }

    @Test
    void executeShouldReportFieldPathWhenFieldExistsButIsFalsy() throws Exception {
        Document doc = Document.fromString("info:\n  name: test\n  tags: \"\"\n", "yaml");
        Rule rule = new Rule("tags-required", "Tags required", null, "warn", true,
                null, null, List.of("$.info"),
                List.of(new RuleAction("tags", "truthy", Map.of())));

        List<Diagnostic> results = executor.execute(rule, doc);
        assertEquals(1, results.size());
        assertEquals("$.info.tags", results.get(0).path(),
                "a present-but-falsy field must be appended to the diagnostic path");
    }

    @Test
    void fieldExistsShouldReturnFalseForNullOrEmptyField() throws Exception {
        JsonNode root = JSON.readTree("{\"info\": {\"name\": \"test\"}}");
        JsonNode info = root.get("info");
        assertFalse(RuleExecutor.fieldExists(info, null));
        assertFalse(RuleExecutor.fieldExists(info, ""));
    }

    @Test
    void fieldExistsShouldReturnFalseForNonObjectMatch() throws Exception {
        JsonNode root = JSON.readTree("{\"info\": \"scalar\"}");
        assertFalse(RuleExecutor.fieldExists(root.get("info"), "tags"));
        assertFalse(RuleExecutor.fieldExists(null, "tags"));
    }

    // --- dotted (nested) field resolution, mirroring Spectral's own given: "$" + nested
    // field: "info.license.url" shape (e.g. info-contact, info-license, license-url) ---

    @Test
    void resolveFieldShouldResolveDottedNestedFieldPath() throws Exception {
        JsonNode match = JSON.readTree("{\"info\": {\"license\": {\"url\": \"https://example.com\"}}}");
        JsonNode result = executor.resolveField(match, "info.license.url");
        assertEquals("https://example.com", result.asText());
    }

    @Test
    void resolveFieldShouldReturnMissingNodeWhenDottedFieldStopsAtMissingSegment() throws Exception {
        JsonNode match = JSON.readTree("{\"info\": {\"title\": \"Test\"}}");
        JsonNode result = executor.resolveField(match, "info.license.url");
        assertTrue(result.isMissingNode());
    }

    @Test
    void fieldExistsShouldReturnTrueWhenEveryDottedSegmentExists() throws Exception {
        JsonNode match = JSON.readTree("{\"info\": {\"license\": {\"url\": \"\"}}}");
        assertTrue(RuleExecutor.fieldExists(match, "info.license.url"));
    }

    @Test
    void fieldExistsShouldReturnFalseWhenAnyDottedSegmentIsMissing() throws Exception {
        JsonNode match = JSON.readTree("{\"info\": {\"title\": \"Test\"}}");
        assertFalse(RuleExecutor.fieldExists(match, "info.license.url"));
    }

    @Test
    void closestExistingFieldPathShouldReturnMatchPathWhenFieldIsNull() {
        assertEquals("$", RuleExecutor.closestExistingFieldPath("$", null, null));
    }

    @Test
    void closestExistingFieldPathShouldReturnMatchPathWhenFieldIsEmpty() throws Exception {
        JsonNode match = JSON.readTree("{\"info\": {}}");
        assertEquals("$", RuleExecutor.closestExistingFieldPath("$", match, ""));
    }

    @Test
    void closestExistingFieldPathShouldStopAtFirstMissingSegment() throws Exception {
        // "info" exists but has no "license" — the path must descend one segment ("info") and
        // stop there, mirroring Spectral's getClosestJsonPath trimming.
        JsonNode match = JSON.readTree("{\"info\": {\"title\": \"Test\"}}");
        assertEquals("$.info", RuleExecutor.closestExistingFieldPath("$", match, "info.license.url"));
    }

    @Test
    void closestExistingFieldPathShouldReturnMatchPathWhenFirstSegmentIsMissing() throws Exception {
        JsonNode match = JSON.readTree("{\"paths\": {}}");
        assertEquals("$", RuleExecutor.closestExistingFieldPath("$", match, "info.license.url"));
    }

    @Test
    void closestExistingFieldPathShouldDescendThroughEveryExistingSegment() throws Exception {
        // Called directly with a field whose every segment exists — the loop must run to
        // completion without ever hitting the missing-segment break, distinct from the
        // execute()-driven callers (which only reach this helper once fieldExists is false).
        JsonNode match = JSON.readTree("{\"info\": {\"license\": {\"url\": \"https://example.com\"}}}");
        assertEquals("$.info.license.url",
                RuleExecutor.closestExistingFieldPath("$", match, "info.license.url"));
    }

    @Test
    void closestExistingFieldPathShouldStopWhenAnIntermediateSegmentIsExplicitJsonNull() throws Exception {
        // "info.license" resolves to an explicit JSON null (not simply missing) — the next
        // segment lookup must see current become a non-object NullNode and stop there.
        JsonNode match = JSON.readTree("{\"info\": {\"license\": null}}");
        assertEquals("$.info.license",
                RuleExecutor.closestExistingFieldPath("$", match, "info.license.url"));
    }

    @Test
    void closestExistingFieldPathShouldReturnMatchPathWhenMatchItselfIsNullAndFieldIsNonEmpty() {
        // The overload's null-match short-circuit (current == null) is only reachable when
        // match itself is null on the very first loop iteration — distinct from a non-null
        // match whose value at some segment is a non-object node.
        assertEquals("$", RuleExecutor.closestExistingFieldPath("$", null, "info.license"));
    }

    @Test
    void executeShouldReportPartialPathWhenDottedFieldStopsAtMissingIntermediateSegment() throws Exception {
        // info-license-equivalent scenario: given "$", field "info.license" — "info" exists
        // but has no "license" key, so the diagnostic must land on $.info, not $.
        Document doc = Document.fromString("info:\n  title: Test\n", "yaml");
        Rule rule = new Rule("info-license", "Info object must have \"license\" object.", null,
                "warn", true, null, null, List.of("$"),
                List.of(new RuleAction("info.license", "truthy", Map.of())));

        List<Diagnostic> results = executor.execute(rule, doc);
        assertEquals(1, results.size());
        assertEquals("$.info", results.get(0).path(),
                "the diagnostic must descend into the existing 'info' prefix but stop before "
                        + "the missing 'license' segment");
    }

    @Test
    void executeShouldReportRootPathWhenDottedFieldsFirstSegmentIsMissing() throws Exception {
        // info-contact-equivalent scenario when "info" itself is entirely absent: given "$",
        // field "info.contact" — no segment of the dotted field exists, so the diagnostic
        // must land on the matched node's own path ($), not descend at all.
        Document doc = Document.fromString("paths: {}\n", "yaml");
        Rule rule = new Rule("info-contact", "Info object must have \"contact\" object.", null,
                "warn", true, null, null, List.of("$"),
                List.of(new RuleAction("info.contact", "truthy", Map.of())));

        List<Diagnostic> results = executor.execute(rule, doc);
        assertEquals(1, results.size());
        assertEquals("$", results.get(0).path(),
                "the diagnostic must stay at the matched node's own path when info is "
                        + "entirely absent");
    }

    @Test
    void executeShouldReportFullDottedFieldPathWhenEverySegmentExists() throws Exception {
        Document doc = Document.fromString("info:\n  license:\n    url: \"\"\n", "yaml");
        Rule rule = new Rule("license-url", "License object must include \"url\".", null,
                "warn", true, null, null, List.of("$"),
                List.of(new RuleAction("info.license.url", "truthy", Map.of())));

        List<Diagnostic> results = executor.execute(rule, doc);
        assertEquals(1, results.size());
        assertEquals("$.info.license.url", results.get(0).path(),
                "a fully-present dotted field must be appended in full to the diagnostic path");
    }

    @Test
    void executeShouldApplyMultipleThenActions() throws Exception {
        JsonNode root = JSON.readTree("{\"path\": \"/users/\"}");
        Rule rule = new Rule("path-rules", "Path violation", null, "warn", true,
                null, null, List.of("$.path"),
                List.of(
                        new RuleAction(null, "pattern", Map.of("notMatch", "/$")),
                        new RuleAction(null, "pattern", Map.of("notMatch", "\\?"))
                ));

        List<Diagnostic> results = executor.execute(rule, root);
        // Only trailing slash matches, not query
        assertEquals(1, results.size());
    }

    @Test
    void executeShouldReturnEmptyWhenNoMatches() throws Exception {
        JsonNode root = JSON.readTree("{\"info\": {\"name\": \"test\"}}");
        Rule rule = new Rule("no-match", "Should not fire", null, "warn", true,
                null, null, List.of("$.nonexistent"),
                List.of(new RuleAction(null, "truthy", Map.of())));

        List<Diagnostic> results = executor.execute(rule, root);
        assertTrue(results.isEmpty());
    }

    @Test
    void executeShouldReturnEmptyWhenTargetValuePassesFunction() throws Exception {
        JsonNode root = JSON.readTree("{\"info\": {\"name\": \"valid-name\"}}");
        Rule rule = new Rule("name-truthy", "Name required", null, "warn", true,
                null, null, List.of("$.info.name"),
                List.of(new RuleAction(null, "truthy", Map.of())));

        List<Diagnostic> results = executor.execute(rule, root);
        assertTrue(results.isEmpty());
    }

    @Test
    void executeShouldSkipUnknownFunction() throws Exception {
        JsonNode root = JSON.readTree("{\"info\": {\"name\": \"test\"}}");
        Rule rule = new Rule("unknown-fn", "Unknown function", null, "warn", true,
                null, null, List.of("$.info.name"),
                List.of(new RuleAction(null, "nonexistent-function", Map.of())));

        List<Diagnostic> results = executor.execute(rule, root);
        assertTrue(results.isEmpty());
    }

    @Test
    void resolveFieldShouldReturnMatchWhenFieldIsNull() throws Exception {
        JsonNode match = JSON.readTree("{\"name\": \"test\"}");
        JsonNode result = executor.resolveField(match, null);
        assertSame(match, result);
    }

    @Test
    void resolveFieldShouldReturnMatchWhenFieldIsEmpty() throws Exception {
        JsonNode match = JSON.readTree("{\"name\": \"test\"}");
        JsonNode result = executor.resolveField(match, "");
        assertSame(match, result);
    }

    @Test
    void resolveFieldShouldReturnFieldValueWhenPresent() throws Exception {
        JsonNode match = JSON.readTree("{\"name\": \"test\"}");
        JsonNode result = executor.resolveField(match, "name");
        assertEquals("test", result.asText());
    }

    @Test
    void resolveFieldShouldReturnMissingNodeWhenFieldAbsent() throws Exception {
        JsonNode match = JSON.readTree("{\"name\": \"test\"}");
        JsonNode result = executor.resolveField(match, "description");
        assertTrue(result.isMissingNode());
    }

    @Test
    void resolveFieldShouldReturnMissingNodeWhenMatchIsNull() {
        JsonNode result = executor.resolveField(null, "name");
        assertTrue(result.isMissingNode());
    }

    @Test
    void resolveFieldShouldReturnMissingNodeWhenMatchIsNotObject() throws Exception {
        JsonNode match = JSON.readTree("\"just a string\"");
        JsonNode result = executor.resolveField(match, "name");
        assertTrue(result.isMissingNode());
    }

    @Test
    void mapSeverityShouldMapAllValidValues() {
        assertEquals(Severity.ERROR, RuleExecutor.mapSeverity("error"));
        assertEquals(Severity.WARN, RuleExecutor.mapSeverity("warn"));
        assertEquals(Severity.INFO, RuleExecutor.mapSeverity("info"));
        assertEquals(Severity.HINT, RuleExecutor.mapSeverity("hint"));
    }

    @Test
    void mapSeverityShouldBeCaseInsensitive() {
        assertEquals(Severity.ERROR, RuleExecutor.mapSeverity("ERROR"));
        assertEquals(Severity.WARN, RuleExecutor.mapSeverity("Warn"));
        assertEquals(Severity.INFO, RuleExecutor.mapSeverity("INFO"));
    }

    @Test
    void mapSeverityShouldDefaultToWarnForNull() {
        assertEquals(Severity.WARN, RuleExecutor.mapSeverity(null));
    }

    @Test
    void mapSeverityShouldDefaultToWarnForUnknown() {
        assertEquals(Severity.WARN, RuleExecutor.mapSeverity("unknown"));
    }

    @Test
    void executeShouldHandleMultipleGivenExpressions() throws Exception {
        String json = """
                {
                  "consumes": [{"baseUri": "https://api.com/"}],
                  "capability": {"consumes": [{"baseUri": "https://other.com/"}]}
                }
                """;
        JsonNode root = JSON.readTree(json);
        Rule rule = new Rule("no-trailing-slash", "No trailing slash", null, "warn", true,
                null, null,
                List.of("$.consumes[*].baseUri", "$.capability.consumes[*].baseUri"),
                List.of(new RuleAction(null, "pattern", Map.of("notMatch", "/$"))));

        List<Diagnostic> results = executor.execute(rule, root);
        assertEquals(2, results.size());
    }

    @Test
    void executeShouldUseFunctionErrorWhenRuleMessageIsNull() throws Exception {
        JsonNode root = JSON.readTree("{\"info\": {\"name\": \"\"}}");
        Rule rule = new Rule("null-msg", null, null, "warn", true,
                null, null, List.of("$.info.name"),
                List.of(new RuleAction(null, "truthy", Map.of())));

        List<Diagnostic> results = executor.execute(rule, root);
        assertEquals(1, results.size());
        assertNotNull(results.get(0).message());
    }

    @Test
    void executeShouldResolveRangeForCustomFunctionViolationWithRelativePath() throws Exception {
        // A custom function reports a violation pinned to a relative path ("name"); the executor
        // must combine it with the matched path and resolve a SourceRange from the document's
        // source map (issue #32, Layer 1).
        FunctionRegistry registry = FunctionRegistry.forRuleset(List.of());
        RuleExecutor pathExecutor = new RuleExecutor(new JsonPathEvaluator(), registry);
        Rule rule = new Rule("path-rule", null, null, "warn", true, null, null,
                List.of("$.info"),
                List.of(new RuleAction(null, "testPathReportingFunction", Map.of())));
        Document doc = Document.fromString("info:\n  name: value\n", "yaml");

        List<Diagnostic> results = pathExecutor.execute(rule, doc);

        assertEquals(1, results.size());
        assertEquals("$.info.name", results.get(0).path());
        assertNotNull(results.get(0).range(),
                "range must resolve from the combined path via the source map");
    }

    @Test
    void executeShouldResolveKeyRangeForKeySelectorRule() throws Exception {
        // A rule whose `given` ends in the key-selector operator (~) must resolve its
        // diagnostic's SourceRange via SourceMap.resolveKey(), pointing at the matched KEY's
        // own location — not the value's — exercising the keySelector branch in
        // RuleExecutor.execute(Rule, Document).
        Rule rule = new Rule("paths-kebab-case", "Path should be kebab-case", null, "warn", true,
                null, null, List.of("$.paths.*~"),
                List.of(new RuleAction(null, "pattern", Map.of("match", "^/[a-z]+$"))));
        String yaml = "paths:\n  /Pets:\n    get: {}\n";
        Document doc = Document.fromString(yaml, "yaml");

        List<Diagnostic> results = executor.execute(rule, doc);

        assertEquals(1, results.size());
        assertEquals("$.paths./Pets", results.get(0).path());
        SourceRange range = results.get(0).range();
        assertNotNull(range, "key-selector diagnostic must carry a SourceRange");
        assertEquals(1, range.startLine(), "range must point at the /Pets KEY (line 2), not its value");
        assertEquals(2, range.startColumn());
        assertEquals(7, range.endColumn(), "range stops before ':', hugging '/Pets'");
    }

    @Test
    void effectivePathShouldReturnMatchPathWhenFieldIsNull() {
        assertEquals("$.info", RuleExecutor.effectivePath("$.info", null));
    }

    @Test
    void effectivePathShouldReturnMatchPathWhenFieldIsEmpty() {
        assertEquals("$.info", RuleExecutor.effectivePath("$.info", ""));
    }

    @Test
    void effectivePathShouldAppendFieldSegmentWhenFieldIsPresent() {
        assertEquals("$.info.name", RuleExecutor.effectivePath("$.info", "name"));
    }

    @Test
    void pathAtShouldReturnConcretePathWhenIndexInRange() {
        assertEquals("$.a[0]", RuleExecutor.pathAt(List.of("$.a[0]", "$.a[1]"), 0, "$.a[*]"));
    }

    @Test
    void pathAtShouldFallBackToGivenWhenIndexOutOfRange() {
        assertEquals("$.a[*]", RuleExecutor.pathAt(List.of(), 0, "$.a[*]"));
    }

    @Test
    void combinePathShouldReturnBaseWhenRelativeIsNull() {
        assertEquals("$.consumes[0]", RuleExecutor.combinePath("$.consumes[0]", null));
    }

    @Test
    void combinePathShouldReturnBaseWhenRelativeIsEmpty() {
        assertEquals("$.consumes[0]", RuleExecutor.combinePath("$.consumes[0]", ""));
    }

    @Test
    void combinePathShouldAppendDottedRelativeSegment() {
        assertEquals("$.consumes[0].namespace",
                RuleExecutor.combinePath("$.consumes[0]", "namespace"));
    }

    @Test
    void combinePathShouldAppendBracketRelativeWithoutDot() {
        assertEquals("$.consumes[0]", RuleExecutor.combinePath("$.consumes", "[0]"));
    }

    // --- message fallback chain: message -> description -> function-generated message,
    // mirroring Spectral (verified against the real Spectral CLI) ---

    @Test
    void executeShouldUseRuleMessageWhenPresent() throws Exception {
        JsonNode root = JSON.readTree("{\"info\": {}}");
        Rule rule = new Rule("info-contact", "Explicit message", "Rule description", "warn", true,
                null, null, List.of("$.info"),
                List.of(new RuleAction("contact", "truthy", Map.of())));

        List<Diagnostic> results = executor.execute(rule, root);
        assertEquals(1, results.size());
        assertEquals("Explicit message", results.get(0).message());
    }

    @Test
    void executeShouldFallBackToDescriptionWhenMessageIsAbsent() throws Exception {
        JsonNode root = JSON.readTree("{\"info\": {}}");
        Rule rule = new Rule("info-contact", null, "Info object must have \"contact\" object.",
                "warn", true, null, null, List.of("$.info"),
                List.of(new RuleAction("contact", "truthy", Map.of())));

        List<Diagnostic> results = executor.execute(rule, root);
        assertEquals(1, results.size());
        assertEquals("Info object must have \"contact\" object.", results.get(0).message(),
                "an absent message must fall back to the rule's description, not a generic "
                        + "function message");
    }

    @Test
    void executeShouldFallBackToFunctionMessageWhenNeitherMessageNorDescriptionIsPresent() throws Exception {
        JsonNode root = JSON.readTree("{\"info\": {}}");
        Rule rule = new Rule("info-contact", null, null, "warn", true,
                null, null, List.of("$.info"),
                List.of(new RuleAction("contact", "truthy", Map.of())));

        List<Diagnostic> results = executor.execute(rule, root);
        assertEquals(1, results.size());
        assertEquals("Value must be truthy", results.get(0).message());
    }

    @Test
    void executeShouldDistinguishTwoRulesSharingMessageFallbackPathAndFunction() throws Exception {
        // Regression: two different rules resolving a missing field to the same matched path,
        // with no `message:` of their own, must NOT collapse into a single diagnostic via
        // Linter's downstream DiagnosticDeduplicator (keyed on path+message) — each rule's own
        // `description:` must distinguish their messages. Discovered via the conformance harness
        // (naftiko/polychro#83): info-contact and info-description both resolve to path "info"
        // when their fields are absent, and neither declares its own `message`.
        JsonNode root = JSON.readTree("{\"info\": {}}");
        Rule infoContact = new Rule("info-contact", null, "Info object must have \"contact\" object.",
                "warn", true, null, null, List.of("$.info"),
                List.of(new RuleAction("contact", "truthy", Map.of())));
        Rule infoDescription = new Rule("info-description", null,
                "Info \"description\" must be present and non-empty string.",
                "warn", true, null, null, List.of("$.info"),
                List.of(new RuleAction("description", "truthy", Map.of())));

        List<Diagnostic> contactResults = executor.execute(infoContact, root);
        List<Diagnostic> descriptionResults = executor.execute(infoDescription, root);

        assertEquals(1, contactResults.size());
        assertEquals(1, descriptionResults.size());
        assertEquals("$.info", contactResults.get(0).path());
        assertEquals("$.info", descriptionResults.get(0).path());
        assertNotEquals(contactResults.get(0).message(), descriptionResults.get(0).message(),
                "each rule's own description must keep the two diagnostics distinguishable "
                        + "even though they share the same path");
    }

    // --- {{error}} message placeholder: opt back into the per-violation function message,
    // mirroring Spectral's own `message: "{{error}}"` convention (e.g. spectral:arazzo) ---

    @Test
    void executeShouldInterpolateErrorPlaceholderWithFunctionMessage() throws Exception {
        JsonNode root = JSON.readTree("{\"info\": {\"name\": \"\"}}");
        Rule rule = new Rule("test-rule", "Validation failed: {{error}}", null, "warn", true,
                null, null, List.of("$.info.name"), List.of(new RuleAction(null, "truthy", Map.of())));

        List<Diagnostic> results = executor.execute(rule, root);
        assertEquals(1, results.size());
        assertEquals("Validation failed: Value must be truthy", results.get(0).message());
    }

    @Test
    void executeShouldPreserveDistinctMessagesForMultiViolationFunctionWithErrorPlaceholder() throws Exception {
        // A custom function that can report multiple distinct violations for the same matched
        // node (like required-sections) must keep each violation's own message distinguishable
        // when the rule opts in via `message: "{{error}}"` — otherwise every violation would
        // collapse to the same static message and only one diagnostic would survive
        // Linter's downstream DiagnosticDeduplicator.
        FunctionRegistry registry = FunctionRegistry.forRuleset(List.of());
        RuleExecutor multiViolationExecutor = new RuleExecutor(new JsonPathEvaluator(), registry);
        JsonNode root = JSON.readTree("{\"info\": {}}");
        Rule rule = new Rule("multi-violation-rule", "{{error}}", null, "warn", true,
                null, null, List.of("$.info"),
                List.of(new RuleAction(null, "testMultiViolationFunction", Map.of())));

        List<Diagnostic> results = multiViolationExecutor.execute(rule, root);

        assertEquals(2, results.size());
        assertNotEquals(results.get(0).message(), results.get(1).message());
    }

    @Test
    void interpolateErrorShouldReturnMessageUnchangedWhenNoPlaceholderPresent() {
        assertEquals("Static message", RuleExecutor.interpolateError("Static message", "some error"));
    }

    @Test
    void interpolateErrorShouldSubstituteEmptyStringWhenErrorMessageIsNull() {
        assertEquals("Prefix: ", RuleExecutor.interpolateError("Prefix: {{error}}", null));
    }
}
