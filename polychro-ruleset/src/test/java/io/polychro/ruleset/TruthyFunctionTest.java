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
