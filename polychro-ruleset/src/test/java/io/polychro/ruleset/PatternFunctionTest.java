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
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PatternFunctionTest {

    private final PatternFunction fn = new PatternFunction();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void nameShouldReturnPattern() {
        assertEquals("pattern", fn.name());
    }

    @Test
    void evaluateShouldPassWhenMatchHits() {
        JsonNode node = mapper.valueToTree("hello-world");
        var result = fn.evaluate(node, Map.of("match", "^hello"));
        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldFailWhenMatchMisses() {
        JsonNode node = mapper.valueToTree("world-hello");
        var result = fn.evaluate(node, Map.of("match", "^hello"));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldPassWhenNotMatchMisses() {
        JsonNode node = mapper.valueToTree("hello-world");
        var result = fn.evaluate(node, Map.of("notMatch", "^world"));
        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldFailWhenNotMatchHits() {
        JsonNode node = mapper.valueToTree("world-hello");
        var result = fn.evaluate(node, Map.of("notMatch", "^world"));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForNonStringValue() {
        JsonNode node = mapper.valueToTree(42);
        var result = fn.evaluate(node, Map.of("match", "\\d+"));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForNullValue() {
        var result = fn.evaluate(null, Map.of("match", ".*"));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForNullNode() {
        JsonNode node = mapper.nullNode();
        var result = fn.evaluate(node, Map.of("match", ".*"));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldReportInvalidMatchRegex() {
        JsonNode node = mapper.valueToTree("test");
        var result = fn.evaluate(node, Map.of("match", "[invalid"));
        assertFalse(result.isEmpty());
        assertTrue(result.get(0).contains("Invalid regex"));
    }

    @Test
    void evaluateShouldReportInvalidNotMatchRegex() {
        JsonNode node = mapper.valueToTree("test");
        var result = fn.evaluate(node, Map.of("notMatch", "[invalid"));
        assertFalse(result.isEmpty());
        assertTrue(result.get(0).contains("Invalid regex"));
    }

    @Test
    void evaluateShouldPassWithNoOptions() {
        JsonNode node = mapper.valueToTree("test");
        var result = fn.evaluate(node, Map.of());
        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForMissingNode() {
        JsonNode node = mapper.missingNode();
        var result = fn.evaluate(node, Map.of("match", ".*"));
        assertFalse(result.isEmpty());
    }
}
