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
