package io.polychro.ruleset;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AlphabeticalFunctionTest {

    private final AlphabeticalFunction fn = new AlphabeticalFunction();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void nameShouldReturnAlphabetical() {
        assertEquals("alphabetical", fn.name());
    }

    @Test
    void evaluateShouldPassForSortedArray() {
        ArrayNode node = mapper.createArrayNode().add("alpha").add("beta").add("gamma");
        var result = fn.evaluate(node, Map.of());
        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForUnsortedArray() {
        ArrayNode node = mapper.createArrayNode().add("gamma").add("alpha").add("beta");
        var result = fn.evaluate(node, Map.of());
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldPassForSortedObjectKeys() {
        // LinkedHashMap preserves order
        ObjectNode node = mapper.createObjectNode();
        node.put("alpha", 1);
        node.put("beta", 2);
        node.put("gamma", 3);
        var result = fn.evaluate(node, Map.of());
        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForUnsortedObjectKeys() {
        ObjectNode node = mapper.createObjectNode();
        node.put("gamma", 1);
        node.put("alpha", 2);
        node.put("beta", 3);
        var result = fn.evaluate(node, Map.of());
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldPassForKeyedByOption() {
        ArrayNode node = mapper.createArrayNode();
        node.add(mapper.createObjectNode().put("name", "alice"));
        node.add(mapper.createObjectNode().put("name", "bob"));
        node.add(mapper.createObjectNode().put("name", "charlie"));
        var result = fn.evaluate(node, Map.of("keyedBy", "name"));
        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForUnsortedKeyedBy() {
        ArrayNode node = mapper.createArrayNode();
        node.add(mapper.createObjectNode().put("name", "charlie"));
        node.add(mapper.createObjectNode().put("name", "alice"));
        var result = fn.evaluate(node, Map.of("keyedBy", "name"));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldPassForSingleElementArray() {
        ArrayNode node = mapper.createArrayNode().add("only");
        var result = fn.evaluate(node, Map.of());
        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldPassForEmptyArray() {
        ArrayNode node = mapper.createArrayNode();
        var result = fn.evaluate(node, Map.of());
        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldPassForSingleKeyObject() {
        ObjectNode node = mapper.createObjectNode().put("only", 1);
        var result = fn.evaluate(node, Map.of());
        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForNullNode() {
        var result = fn.evaluate(null, Map.of());
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForJsonNull() {
        JsonNode node = mapper.nullNode();
        var result = fn.evaluate(node, Map.of());
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForNonArrayOrObject() {
        JsonNode node = mapper.valueToTree("string");
        var result = fn.evaluate(node, Map.of());
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldFailWhenKeyedByFieldMissing() {
        ArrayNode node = mapper.createArrayNode();
        node.add(mapper.createObjectNode().put("name", "alice"));
        node.add(mapper.createObjectNode().put("other", "bob"));
        var result = fn.evaluate(node, Map.of("keyedBy", "name"));
        assertFalse(result.isEmpty());
        assertTrue(result.get(0).contains("missing field"));
    }

    @Test
    void evaluateShouldHandleNonTextualArrayElements() {
        ArrayNode node = mapper.createArrayNode().add(1).add(2).add(3);
        var result = fn.evaluate(node, Map.of());
        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForMissingNode() {
        JsonNode node = mapper.missingNode();
        var result = fn.evaluate(node, Map.of());
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldPassForEmptyObject() {
        ObjectNode node = mapper.createObjectNode();
        var result = fn.evaluate(node, Map.of());
        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldFailWhenKeyedByFieldIsNonTextual() {
        ArrayNode node = mapper.createArrayNode();
        node.add(mapper.createObjectNode().put("name", "alice"));
        node.add(mapper.createObjectNode().put("name", 42));
        var result = fn.evaluate(node, Map.of("keyedBy", "name"));
        assertFalse(result.isEmpty());
        assertTrue(result.get(0).contains("missing field"));
    }
}
