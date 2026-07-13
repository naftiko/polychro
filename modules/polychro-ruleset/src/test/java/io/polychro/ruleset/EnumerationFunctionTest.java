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

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EnumerationFunctionTest {

    private final EnumerationFunction fn = new EnumerationFunction();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void nameShouldReturnEnumeration() {
        assertEquals("enumeration", fn.name());
    }

    @Test
    void evaluateShouldPassWhenValueInList() {
        JsonNode node = mapper.valueToTree("active");
        var result = fn.evaluate(node, Map.of("values", List.of("active", "inactive")));
        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldFailWhenValueNotInList() {
        JsonNode node = mapper.valueToTree("unknown");
        var result = fn.evaluate(node, Map.of("values", List.of("active", "inactive")));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForNullValue() {
        var result = fn.evaluate(null, Map.of("values", List.of("a", "b")));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForNullNode() {
        JsonNode node = mapper.nullNode();
        var result = fn.evaluate(node, Map.of("values", List.of("a", "b")));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForEmptyList() {
        JsonNode node = mapper.valueToTree("test");
        var result = fn.evaluate(node, Map.of("values", List.of()));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldFailWhenNoValuesOption() {
        JsonNode node = mapper.valueToTree("test");
        var result = fn.evaluate(node, Map.of());
        assertFalse(result.isEmpty());
        assertTrue(result.get(0).contains("'values' option"));
    }

    @Test
    void evaluateShouldFailWhenValuesNotAList() {
        JsonNode node = mapper.valueToTree("test");
        var result = fn.evaluate(node, Map.of("values", "not-a-list"));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForMissingNode() {
        JsonNode node = mapper.missingNode();
        var result = fn.evaluate(node, Map.of("values", List.of("a")));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldHandleNullItemInValues() {
        JsonNode node = mapper.valueToTree("active");
        List<Object> values = new java.util.ArrayList<>();
        values.add(null);
        values.add("active");
        var result = fn.evaluate(node, Map.of("values", values));
        assertTrue(result.isEmpty());
    }
}
