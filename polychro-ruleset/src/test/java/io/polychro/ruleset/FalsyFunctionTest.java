package io.polychro.ruleset;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FalsyFunctionTest {

    private final FalsyFunction fn = new FalsyFunction();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void nameShouldReturnFalsy() {
        assertEquals("falsy", fn.name());
    }

    @Test
    void evaluateShouldPassForNull() {
        JsonNode node = mapper.nullNode();
        assertTrue(fn.evaluate(node, Map.of()).isEmpty());
    }

    @Test
    void evaluateShouldPassForJavaNull() {
        assertTrue(fn.evaluate(null, Map.of()).isEmpty());
    }

    @Test
    void evaluateShouldPassForEmptyString() {
        JsonNode node = mapper.valueToTree("");
        assertTrue(fn.evaluate(node, Map.of()).isEmpty());
    }

    @Test
    void evaluateShouldPassForZero() {
        JsonNode node = mapper.valueToTree(0);
        assertTrue(fn.evaluate(node, Map.of()).isEmpty());
    }

    @Test
    void evaluateShouldPassForFalse() {
        JsonNode node = mapper.valueToTree(false);
        assertTrue(fn.evaluate(node, Map.of()).isEmpty());
    }

    @Test
    void evaluateShouldPassForEmptyArray() {
        ArrayNode node = mapper.createArrayNode();
        assertTrue(fn.evaluate(node, Map.of()).isEmpty());
    }

    @Test
    void evaluateShouldPassForEmptyObject() {
        ObjectNode node = mapper.createObjectNode();
        assertTrue(fn.evaluate(node, Map.of()).isEmpty());
    }

    @Test
    void evaluateShouldFailForNonEmptyString() {
        JsonNode node = mapper.valueToTree("hello");
        assertFalse(fn.evaluate(node, Map.of()).isEmpty());
    }

    @Test
    void evaluateShouldFailForNonZeroNumber() {
        JsonNode node = mapper.valueToTree(42);
        assertFalse(fn.evaluate(node, Map.of()).isEmpty());
    }

    @Test
    void evaluateShouldFailForTrue() {
        JsonNode node = mapper.valueToTree(true);
        assertFalse(fn.evaluate(node, Map.of()).isEmpty());
    }

    @Test
    void evaluateShouldPassForMissingNode() {
        JsonNode node = mapper.missingNode();
        assertTrue(fn.evaluate(node, Map.of()).isEmpty());
    }
}
