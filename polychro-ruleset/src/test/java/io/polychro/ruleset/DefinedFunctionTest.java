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
