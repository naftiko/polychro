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
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonPathEvaluatorTest {

    private JsonPathEvaluator evaluator;
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
    private static final ObjectMapper JSON = new ObjectMapper();

    @BeforeEach
    void setUp() {
        evaluator = new JsonPathEvaluator();
    }

    @Test
    void evaluateShouldReturnMatchForSimplePath() throws Exception {
        JsonNode root = JSON.readTree("{\"info\": {\"name\": \"test-cap\"}}");
        List<JsonNode> results = evaluator.evaluate(root, "$.info.name");
        assertEquals(1, results.size());
        assertEquals("test-cap", results.get(0).asText());
    }

    @Test
    void evaluateShouldReturnMatchForArrayIndex() throws Exception {
        JsonNode root = JSON.readTree("{\"items\": [\"a\", \"b\", \"c\"]}");
        List<JsonNode> results = evaluator.evaluate(root, "$.items[1]");
        assertEquals(1, results.size());
        assertEquals("b", results.get(0).asText());
    }

    @Test
    void evaluateShouldReturnAllMatchesForWildcard() throws Exception {
        JsonNode root = JSON.readTree("{\"items\": [\"a\", \"b\", \"c\"]}");
        List<JsonNode> results = evaluator.evaluate(root, "$.items[*]");
        assertEquals(3, results.size());
    }

    @Test
    void evaluateShouldReturnMatchesForRecursiveDescent() throws Exception {
        JsonNode root = JSON.readTree("{\"a\": {\"name\": \"x\"}, \"b\": {\"name\": \"y\"}}");
        List<JsonNode> results = evaluator.evaluate(root, "$..name");
        assertEquals(2, results.size());
    }

    @Test
    void evaluateShouldReturnMatchesForFilterExpression() throws Exception {
        String yaml = """
                adapters:
                  - type: rest
                    port: 8080
                  - type: mcp
                    port: 9090
                """;
        JsonNode root = YAML.readTree(yaml);
        List<JsonNode> results = evaluator.evaluate(root, "$.adapters[?(@.type == 'rest')]");
        assertEquals(1, results.size());
        assertEquals("rest", results.get(0).get("type").asText());
    }

    @Test
    void evaluateShouldReturnEmptyListWhenNoMatches() throws Exception {
        JsonNode root = JSON.readTree("{\"info\": {\"name\": \"test\"}}");
        List<JsonNode> results = evaluator.evaluate(root, "$.nonexistent.path");
        assertTrue(results.isEmpty());
    }

    @Test
    void evaluateShouldReturnEmptyListForInvalidPath() throws Exception {
        JsonNode root = JSON.readTree("{\"info\": {}}");
        List<JsonNode> results = evaluator.evaluate(root, "$[invalid[[");
        assertTrue(results.isEmpty());
    }

    @Test
    void evaluateShouldReturnEmptyListForNullDocument() {
        List<JsonNode> results = evaluator.evaluate(null, "$.info");
        assertTrue(results.isEmpty());
    }

    @Test
    void evaluateShouldReturnEmptyListForNullExpression() throws Exception {
        JsonNode root = JSON.readTree("{\"info\": {}}");
        List<JsonNode> results = evaluator.evaluate(root, null);
        assertTrue(results.isEmpty());
    }

    @Test
    void evaluateShouldReturnEmptyListForBlankExpression() throws Exception {
        JsonNode root = JSON.readTree("{\"info\": {}}");
        List<JsonNode> results = evaluator.evaluate(root, "   ");
        assertTrue(results.isEmpty());
    }

    @Test
    void evaluateShouldReturnRootForDollarOnly() throws Exception {
        JsonNode root = JSON.readTree("{\"info\": {\"name\": \"test\"}}");
        List<JsonNode> results = evaluator.evaluate(root, "$");
        assertEquals(1, results.size());
        assertTrue(results.get(0).has("info"));
    }

    @Test
    void evaluateShouldHandleNestedArrays() throws Exception {
        String json = """
                {
                  "capability": {
                    "consumes": [
                      {"baseUri": "https://api.example.com/"},
                      {"baseUri": "https://other.com"}
                    ]
                  }
                }
                """;
        JsonNode root = JSON.readTree(json);
        List<JsonNode> results = evaluator.evaluate(root, "$.capability.consumes[*].baseUri");
        assertEquals(2, results.size());
        assertEquals("https://api.example.com/", results.get(0).asText());
        assertEquals("https://other.com", results.get(1).asText());
    }

    // --- Issue #32: concrete-path resolution (evaluatePaths + toDotNotation) ---

    @Test
    void evaluatePathsShouldReturnConcreteDotNotationPathsForWildcard() throws Exception {
        String yaml = "consumes:\n  - baseUri: a\n  - baseUri: b\n";
        JsonNode root = YAML.readTree(yaml);
        List<String> paths = evaluator.evaluatePaths(root, "$.consumes[*].baseUri");
        assertEquals(List.of("$.consumes[0].baseUri", "$.consumes[1].baseUri"), paths);
    }

    @Test
    void evaluatePathsShouldReturnSinglePathForScalar() throws Exception {
        JsonNode root = JSON.readTree("{\"info\": {\"name\": \"x\"}}");
        List<String> paths = evaluator.evaluatePaths(root, "$.info.name");
        assertEquals(List.of("$.info.name"), paths);
    }

    @Test
    void evaluatePathsShouldReturnEmptyForNullDocument() {
        assertTrue(evaluator.evaluatePaths(null, "$.info").isEmpty());
    }

    @Test
    void evaluatePathsShouldReturnEmptyForNullExpression() throws Exception {
        JsonNode root = JSON.readTree("{\"info\": {}}");
        assertTrue(evaluator.evaluatePaths(root, null).isEmpty());
    }

    @Test
    void evaluatePathsShouldReturnEmptyForBlankExpression() throws Exception {
        JsonNode root = JSON.readTree("{\"info\": {}}");
        assertTrue(evaluator.evaluatePaths(root, "   ").isEmpty());
    }

    @Test
    void evaluatePathsShouldReturnEmptyForInvalidExpression() throws Exception {
        JsonNode root = JSON.readTree("{\"info\": {}}");
        assertTrue(evaluator.evaluatePaths(root, "$[invalid[[").isEmpty());
    }

    @Test
    void toDotNotationShouldConvertBracketKeysAndIndexes() {
        assertEquals("$.consumes[0].baseUri",
                JsonPathEvaluator.toDotNotation("$['consumes'][0]['baseUri']"));
    }

    @Test
    void toDotNotationShouldReturnInputForNull() {
        assertNull(JsonPathEvaluator.toDotNotation(null));
    }

    @Test
    void toDotNotationShouldReturnInputForEmptyString() {
        assertEquals("", JsonPathEvaluator.toDotNotation(""));
    }

    // --- Issue #83: JSONPath key selector (~) ---

    @Test
    void evaluateShouldReturnKeysForWildcardKeySelector() throws Exception {
        String yaml = "paths:\n  /pets:\n    get: {}\n  /pets/{id}:\n    get: {}\n";
        JsonNode root = YAML.readTree(yaml);
        List<JsonNode> results = evaluator.evaluate(root, "$.paths.*~");
        assertEquals(List.of("/pets", "/pets/{id}"),
                results.stream().map(JsonNode::asText).toList());
    }

    @Test
    void evaluateShouldReturnSingleKeyForSimplePathKeySelector() throws Exception {
        JsonNode root = JSON.readTree("{\"info\": {\"name\": \"x\"}}");
        List<JsonNode> results = evaluator.evaluate(root, "$~");
        // $ has no owning key in its parent — root key-selector degrades to a JSON null,
        // mirroring jsonpath-plus's undefined/null result for $~.
        assertEquals(1, results.size());
        assertTrue(results.get(0).isNull());
    }

    @Test
    void evaluateShouldReturnNumericIndexAsIntNodeForArrayKeySelector() throws Exception {
        JsonNode root = JSON.readTree("{\"items\": [\"a\", \"b\", \"c\"]}");
        List<JsonNode> results = evaluator.evaluate(root, "$.items[*]~");
        assertEquals(3, results.size());
        assertTrue(results.get(0).isInt());
        assertEquals(List.of(0, 1, 2), results.stream().map(JsonNode::asInt).toList());
    }

    @Test
    void evaluateShouldTolerateWhitespaceAfterKeySelectorOperator() throws Exception {
        String yaml = "paths:\n  /pets: {}\n";
        JsonNode root = YAML.readTree(yaml);
        List<JsonNode> results = evaluator.evaluate(root, "$.paths.*~  ");
        assertEquals(List.of("/pets"), results.stream().map(JsonNode::asText).toList());
    }

    @Test
    void evaluateShouldReturnEmptyListForKeySelectorWithNoMatches() throws Exception {
        JsonNode root = JSON.readTree("{\"info\": {}}");
        List<JsonNode> results = evaluator.evaluate(root, "$.nonexistent.*~");
        assertTrue(results.isEmpty());
    }

    @Test
    void evaluatePathsShouldReturnUnderlyingNodePathsForKeySelector() throws Exception {
        String yaml = "paths:\n  /pets: {}\n  /pets/{id}: {}\n";
        JsonNode root = YAML.readTree(yaml);
        List<String> paths = evaluator.evaluatePaths(root, "$.paths.*~");
        assertEquals(List.of("$.paths./pets", "$.paths./pets/{id}"), paths);
    }

    @Test
    void evaluateShouldReturnEmptyListForInvalidKeySelectorExpression() throws Exception {
        // Malformed base expression under a key selector must degrade to an empty list rather
        // than propagate — exercises rawPaths' catch(Exception) branch.
        JsonNode root = JSON.readTree("{\"info\": {}}");
        List<JsonNode> results = evaluator.evaluate(root, "$[invalid[[~");
        assertTrue(results.isEmpty());
    }

    @Test
    void isKeySelectorShouldDetectTrailingTilde() {
        assertTrue(JsonPathEvaluator.isKeySelector("$.paths.*~"));
        assertTrue(JsonPathEvaluator.isKeySelector("$.paths.*~  "));
        assertFalse(JsonPathEvaluator.isKeySelector("$.paths.*"));
        assertFalse(JsonPathEvaluator.isKeySelector(null));
    }

    @Test
    void stripKeySelectorShouldRemoveTrailingTilde() {
        assertEquals("$.paths.*", JsonPathEvaluator.stripKeySelector("$.paths.*~"));
        assertEquals("$.paths.*", JsonPathEvaluator.stripKeySelector("$.paths.*~  "));
    }

    @Test
    void lastSegmentAsNodeShouldExtractTrailingStringKey() {
        JsonNode node = JsonPathEvaluator.lastSegmentAsNode("$['paths']['/pets']");
        assertTrue(node.isTextual());
        assertEquals("/pets", node.asText());
    }

    @Test
    void lastSegmentAsNodeShouldExtractTrailingNumericIndex() {
        JsonNode node = JsonPathEvaluator.lastSegmentAsNode("$['items'][2]");
        assertTrue(node.isInt());
        assertEquals(2, node.asInt());
    }

    @Test
    void lastSegmentAsNodeShouldReturnJsonNullForRootPath() {
        // jsonpath-plus returns the root's (nonexistent) parent-property value — undefined/null —
        // for a segmentless path; an empty TextNode would instead be a truthy, matchable string
        // and silently change truthy/pattern/etc. results for this degenerate case.
        JsonNode node = JsonPathEvaluator.lastSegmentAsNode("$");
        assertTrue(node.isNull());
    }

    // --- BRACKET_SEGMENT must tolerate unescaped `'` inside a key ---
    // Jayway 2.9.0 builds AS_PATH_LIST entries by inserting property names directly into
    // ['...'] without escaping embedded single quotes, so a key such as "/owner's-pets"
    // serializes as the raw segment "['/owner's-pets']".

    @Test
    void lastSegmentAsNodeShouldExtractKeyContainingSingleQuote() {
        JsonNode node = JsonPathEvaluator.lastSegmentAsNode("$['paths']['/owner's-pets']");
        assertTrue(node.isTextual());
        assertEquals("/owner's-pets", node.asText());
    }

    @Test
    void toDotNotationShouldConvertKeyContainingSingleQuote() {
        assertEquals("$.paths./owner's-pets",
                JsonPathEvaluator.toDotNotation("$['paths']['/owner's-pets']"));
    }

    @Test
    void evaluateShouldReturnKeyContainingSingleQuoteForKeySelector() throws Exception {
        String yaml = "paths:\n  \"/owner's-pets\":\n    get: {}\n";
        JsonNode root = YAML.readTree(yaml);
        List<JsonNode> results = evaluator.evaluate(root, "$.paths.*~");
        assertEquals(List.of("/owner's-pets"),
                results.stream().map(JsonNode::asText).toList());
    }
}
