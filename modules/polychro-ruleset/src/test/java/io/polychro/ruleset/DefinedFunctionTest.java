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

class DefinedFunctionTest {

    private final DefinedFunction fn = new DefinedFunction();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void nameShouldReturnDefined() {
        assertEquals("defined", fn.name());
    }

    @Test
    void evaluateShouldPassForPresentNode() {
        JsonNode node = mapper.valueToTree("hello");
        assertTrue(fn.evaluate(node, Map.of()).isEmpty());
    }

    @Test
    void evaluateShouldPassForNullValueNode() {
        JsonNode node = mapper.nullNode();
        assertTrue(fn.evaluate(node, Map.of()).isEmpty());
    }

    @Test
    void evaluateShouldFailForJavaNull() {
        assertFalse(fn.evaluate(null, Map.of()).isEmpty());
    }

    @Test
    void evaluateShouldFailForMissingNode() {
        JsonNode node = mapper.missingNode();
        assertFalse(fn.evaluate(node, Map.of()).isEmpty());
    }

    @Test
    void evaluateShouldPassForEmptyObject() {
        JsonNode node = mapper.createObjectNode();
        assertTrue(fn.evaluate(node, Map.of()).isEmpty());
    }
}
