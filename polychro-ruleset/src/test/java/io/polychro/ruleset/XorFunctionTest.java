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
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class XorFunctionTest {

    private final XorFunction fn = new XorFunction();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void nameShouldReturnXor() {
        assertEquals("xor", fn.name());
    }

    @Test
    void evaluateShouldPassWhenExactlyOnePresent() {
        ObjectNode node = mapper.createObjectNode().put("a", "value");
        var result = fn.evaluate(node, Map.of("properties", List.of("a", "b")));
        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldFailWhenNonePresent() {
        ObjectNode node = mapper.createObjectNode().put("c", "value");
        var result = fn.evaluate(node, Map.of("properties", List.of("a", "b")));
        assertFalse(result.isEmpty());
        assertTrue(result.get(0).contains("none were found"));
    }

    @Test
    void evaluateShouldFailWhenBothPresent() {
        ObjectNode node = mapper.createObjectNode().put("a", "v1").put("b", "v2");
        var result = fn.evaluate(node, Map.of("properties", List.of("a", "b")));
        assertFalse(result.isEmpty());
        assertTrue(result.get(0).contains("2 were found"));
    }

    @Test
    void evaluateShouldHandleMoreThanTwoProperties() {
        ObjectNode node = mapper.createObjectNode().put("b", "value");
        var result = fn.evaluate(node, Map.of("properties", List.of("a", "b", "c")));
        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldFailWhenMultipleOfThreePresent() {
        ObjectNode node = mapper.createObjectNode().put("a", "v1").put("c", "v3");
        var result = fn.evaluate(node, Map.of("properties", List.of("a", "b", "c")));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForNullNode() {
        var result = fn.evaluate(null, Map.of("properties", List.of("a", "b")));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForJsonNull() {
        JsonNode node = mapper.nullNode();
        var result = fn.evaluate(node, Map.of("properties", List.of("a", "b")));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForNonObject() {
        JsonNode node = mapper.valueToTree("string");
        var result = fn.evaluate(node, Map.of("properties", List.of("a", "b")));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldFailWhenNoPropertiesOption() {
        ObjectNode node = mapper.createObjectNode().put("a", "value");
        var result = fn.evaluate(node, Map.of());
        assertFalse(result.isEmpty());
        assertTrue(result.get(0).contains("'properties' option"));
    }

    @Test
    void evaluateShouldIgnoreNullValuedProperties() {
        ObjectNode node = mapper.createObjectNode();
        node.putNull("a");
        node.put("b", "value");
        var result = fn.evaluate(node, Map.of("properties", List.of("a", "b")));
        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForMissingNode() {
        JsonNode node = mapper.missingNode();
        var result = fn.evaluate(node, Map.of("properties", List.of("a", "b")));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldHandleNullPropertyInList() {
        ObjectNode node = mapper.createObjectNode().put("a", "value");
        List<Object> props = new java.util.ArrayList<>();
        props.add(null);
        props.add("a");
        var result = fn.evaluate(node, Map.of("properties", props));
        assertTrue(result.isEmpty());
    }
}
