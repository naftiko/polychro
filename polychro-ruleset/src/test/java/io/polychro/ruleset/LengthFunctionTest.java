package io.polychro.ruleset;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LengthFunctionTest {

    private final LengthFunction fn = new LengthFunction();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void nameShouldReturnLength() {
        assertEquals("length", fn.name());
    }

    @Test
    void evaluateShouldPassForStringWithinMinMax() {
        JsonNode node = mapper.valueToTree("hello");
        var result = fn.evaluate(node, Map.of("min", 1, "max", 10));
        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForStringBelowMin() {
        JsonNode node = mapper.valueToTree("hi");
        var result = fn.evaluate(node, Map.of("min", 5));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForStringAboveMax() {
        JsonNode node = mapper.valueToTree("hello world");
        var result = fn.evaluate(node, Map.of("max", 5));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldPassForArrayWithinMin() {
        ArrayNode node = mapper.createArrayNode().add("a").add("b");
        var result = fn.evaluate(node, Map.of("min", 2));
        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForArrayBelowMin() {
        ArrayNode node = mapper.createArrayNode().add("a");
        var result = fn.evaluate(node, Map.of("min", 2));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldPassForArrayWithinMax() {
        ArrayNode node = mapper.createArrayNode().add("a").add("b");
        var result = fn.evaluate(node, Map.of("max", 3));
        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForArrayAboveMax() {
        ArrayNode node = mapper.createArrayNode().add("a").add("b").add("c");
        var result = fn.evaluate(node, Map.of("max", 2));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldPassForObjectKeyCountWithinMin() {
        ObjectNode node = mapper.createObjectNode().put("a", 1).put("b", 2);
        var result = fn.evaluate(node, Map.of("min", 2));
        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForObjectKeyCountAboveMax() {
        ObjectNode node = mapper.createObjectNode().put("a", 1).put("b", 2).put("c", 3);
        var result = fn.evaluate(node, Map.of("max", 2));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldPassAtExactBoundary() {
        JsonNode node = mapper.valueToTree("hello");
        var result = fn.evaluate(node, Map.of("min", 5, "max", 5));
        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForNullNode() {
        var result = fn.evaluate(null, Map.of("min", 1));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForJsonNull() {
        JsonNode node = mapper.nullNode();
        var result = fn.evaluate(node, Map.of("min", 1));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForNumberNode() {
        JsonNode node = mapper.valueToTree(42);
        var result = fn.evaluate(node, Map.of("min", 1));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldHandleStringMinOption() {
        JsonNode node = mapper.valueToTree("hello");
        var result = fn.evaluate(node, Map.of("min", "3"));
        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldHandleInvalidMinOption() {
        JsonNode node = mapper.valueToTree("hello");
        var result = fn.evaluate(node, Map.of("min", "notanumber"));
        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldPassWithNoOptions() {
        JsonNode node = mapper.valueToTree("hello");
        var result = fn.evaluate(node, Map.of());
        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForMissingNode() {
        JsonNode node = mapper.missingNode();
        var result = fn.evaluate(node, Map.of("min", 1));
        assertFalse(result.isEmpty());
    }
}
