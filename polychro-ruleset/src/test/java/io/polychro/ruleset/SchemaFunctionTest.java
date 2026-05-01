package io.polychro.ruleset;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SchemaFunctionTest {

    private final SchemaFunction fn = new SchemaFunction();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void nameShouldReturnSchema() {
        assertEquals("schema", fn.name());
    }

    @Test
    void evaluateShouldPassForValidNode() {
        ObjectNode node = mapper.createObjectNode().put("name", "test");
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of("name", Map.of("type", "string")),
                "required", java.util.List.of("name")
        );
        var result = fn.evaluate(node, Map.of("schema", schema));
        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForInvalidNode() {
        ObjectNode node = mapper.createObjectNode().put("name", 42);
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of("name", Map.of("type", "string"))
        );
        var result = fn.evaluate(node, Map.of("schema", schema));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForNullNode() {
        var result = fn.evaluate(null, Map.of("schema", Map.of("type", "object")));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForJsonNull() {
        JsonNode node = mapper.nullNode();
        var result = fn.evaluate(node, Map.of("schema", Map.of("type", "object")));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldFailWhenNoSchemaOption() {
        ObjectNode node = mapper.createObjectNode().put("name", "test");
        var result = fn.evaluate(node, Map.of());
        assertFalse(result.isEmpty());
        assertTrue(result.get(0).contains("'schema' option"));
    }

    @Test
    void evaluateShouldFailWhenSchemaNotAMap() {
        ObjectNode node = mapper.createObjectNode().put("name", "test");
        var result = fn.evaluate(node, Map.of("schema", "not-a-map"));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldValidateNestedSchema() {
        ObjectNode inner = mapper.createObjectNode().put("city", "Paris");
        ObjectNode node = mapper.createObjectNode();
        node.set("address", inner);

        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "address", Map.of(
                                "type", "object",
                                "properties", Map.of("city", Map.of("type", "string"))
                        )
                )
        );
        var result = fn.evaluate(node, Map.of("schema", schema));
        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForMissingNode() {
        JsonNode node = mapper.missingNode();
        var result = fn.evaluate(node, Map.of("schema", Map.of("type", "string")));
        assertFalse(result.isEmpty());
    }
}
