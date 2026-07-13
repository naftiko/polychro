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

class UndefinedFunctionTest {

    private final UndefinedFunction fn = new UndefinedFunction();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void nameShouldReturnUndefined() {
        assertEquals("undefined", fn.name());
    }

    @Test
    void evaluateShouldPassForJavaNull() {
        assertTrue(fn.evaluate(null, Map.of()).isEmpty());
    }

    @Test
    void evaluateShouldPassForMissingNode() {
        JsonNode node = mapper.missingNode();
        assertTrue(fn.evaluate(node, Map.of()).isEmpty());
    }

    @Test
    void evaluateShouldFailForPresentNode() {
        JsonNode node = mapper.valueToTree("hello");
        assertFalse(fn.evaluate(node, Map.of()).isEmpty());
    }

    @Test
    void evaluateShouldFailForNullValueNode() {
        JsonNode node = mapper.nullNode();
        assertFalse(fn.evaluate(node, Map.of()).isEmpty());
    }

    @Test
    void evaluateShouldFailForEmptyObject() {
        JsonNode node = mapper.createObjectNode();
        assertFalse(fn.evaluate(node, Map.of()).isEmpty());
    }
}
