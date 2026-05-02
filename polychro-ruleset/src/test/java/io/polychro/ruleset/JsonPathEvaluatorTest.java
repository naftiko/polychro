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
}
