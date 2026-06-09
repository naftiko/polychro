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
        FunctionRegistry registry = FunctionRegistry.forRuleset(null, List.of());
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
}
