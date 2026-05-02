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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TruthyFunctionTest {

    private final TruthyFunction fn = new TruthyFunction();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void nameShouldReturnTruthy() {
        assertEquals("truthy", fn.name());
    }

    @Test
    void evaluateShouldPassForNonNullString() {
        JsonNode node = mapper.valueToTree("hello");
        assertTrue(fn.evaluate(node, Map.of()).isEmpty());
    }

    @Test
    void evaluateShouldFailForEmptyString() {
        JsonNode node = mapper.valueToTree("");
        assertFalse(fn.evaluate(node, Map.of()).isEmpty());
    }

    @Test
    void evaluateShouldFailForZero() {
        JsonNode node = mapper.valueToTree(0);
        assertFalse(fn.evaluate(node, Map.of()).isEmpty());
    }

    @Test
    void evaluateShouldFailForFalse() {
        JsonNode node = mapper.valueToTree(false);
        assertFalse(fn.evaluate(node, Map.of()).isEmpty());
    }

    @Test
    void evaluateShouldFailForNull() {
        JsonNode node = mapper.nullNode();
        assertFalse(fn.evaluate(node, Map.of()).isEmpty());
    }

    @Test
    void evaluateShouldFailForJavaNull() {
        assertFalse(fn.evaluate(null, Map.of()).isEmpty());
    }

    @Test
    void evaluateShouldFailForEmptyArray() {
        ArrayNode node = mapper.createArrayNode();
        assertFalse(fn.evaluate(node, Map.of()).isEmpty());
    }

    @Test
    void evaluateShouldFailForEmptyObject() {
        ObjectNode node = mapper.createObjectNode();
        assertFalse(fn.evaluate(node, Map.of()).isEmpty());
    }

    @Test
    void evaluateShouldPassForNonEmptyArray() {
        ArrayNode node = mapper.createArrayNode().add("item");
        assertTrue(fn.evaluate(node, Map.of()).isEmpty());
    }

    @Test
    void evaluateShouldPassForNonEmptyObject() {
        ObjectNode node = mapper.createObjectNode().put("key", "value");
        assertTrue(fn.evaluate(node, Map.of()).isEmpty());
    }

    @Test
    void evaluateShouldPassForNonZeroNumber() {
        JsonNode node = mapper.valueToTree(42);
        assertTrue(fn.evaluate(node, Map.of()).isEmpty());
    }

    @Test
    void evaluateShouldPassForTrue() {
        JsonNode node = mapper.valueToTree(true);
        assertTrue(fn.evaluate(node, Map.of()).isEmpty());
    }

    @Test
    void evaluateShouldFailForMissingNode() {
        JsonNode node = mapper.missingNode();
        assertFalse(fn.evaluate(node, Map.of()).isEmpty());
    }
}
