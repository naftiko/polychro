package io.polychro.ruleset;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CasingFunctionTest {

    private final CasingFunction fn = new CasingFunction();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void nameShouldReturnCasing() {
        assertEquals("casing", fn.name());
    }

    @Test
    void evaluateShouldPassForValidCamelCase() {
        JsonNode node = mapper.valueToTree("myVariable");
        var result = fn.evaluate(node, Map.of("type", "camel"));
        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForInvalidCamelCase() {
        JsonNode node = mapper.valueToTree("MyVariable");
        var result = fn.evaluate(node, Map.of("type", "camel"));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldPassForValidPascalCase() {
        JsonNode node = mapper.valueToTree("MyVariable");
        var result = fn.evaluate(node, Map.of("type", "pascal"));
        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForInvalidPascalCase() {
        JsonNode node = mapper.valueToTree("myVariable");
        var result = fn.evaluate(node, Map.of("type", "pascal"));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldPassForValidKebabCase() {
        JsonNode node = mapper.valueToTree("my-variable");
        var result = fn.evaluate(node, Map.of("type", "kebab"));
        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForInvalidKebabCase() {
        JsonNode node = mapper.valueToTree("my_variable");
        var result = fn.evaluate(node, Map.of("type", "kebab"));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldPassForValidSnakeCase() {
        JsonNode node = mapper.valueToTree("my_variable");
        var result = fn.evaluate(node, Map.of("type", "snake"));
        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForInvalidSnakeCase() {
        JsonNode node = mapper.valueToTree("my-variable");
        var result = fn.evaluate(node, Map.of("type", "snake"));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldPassForValidCobolCase() {
        JsonNode node = mapper.valueToTree("MY-VARIABLE");
        var result = fn.evaluate(node, Map.of("type", "cobol"));
        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForInvalidCobolCase() {
        JsonNode node = mapper.valueToTree("my-variable");
        var result = fn.evaluate(node, Map.of("type", "cobol"));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldPassForValidMacroCase() {
        JsonNode node = mapper.valueToTree("MY_VARIABLE");
        var result = fn.evaluate(node, Map.of("type", "macro"));
        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForInvalidMacroCase() {
        JsonNode node = mapper.valueToTree("my_variable");
        var result = fn.evaluate(node, Map.of("type", "macro"));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForNullNode() {
        var result = fn.evaluate(null, Map.of("type", "camel"));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForJsonNull() {
        JsonNode node = mapper.nullNode();
        var result = fn.evaluate(node, Map.of("type", "camel"));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForNonString() {
        JsonNode node = mapper.valueToTree(42);
        var result = fn.evaluate(node, Map.of("type", "camel"));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldPassForEmptyString() {
        JsonNode node = mapper.valueToTree("");
        var result = fn.evaluate(node, Map.of("type", "camel"));
        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldFailWhenNoTypeOption() {
        JsonNode node = mapper.valueToTree("test");
        var result = fn.evaluate(node, Map.of());
        assertFalse(result.isEmpty());
        assertTrue(result.get(0).contains("'type' option"));
    }

    @Test
    void evaluateShouldFailForUnknownType() {
        JsonNode node = mapper.valueToTree("test");
        var result = fn.evaluate(node, Map.of("type", "unknown"));
        assertFalse(result.isEmpty());
        assertTrue(result.get(0).contains("Unknown casing type"));
    }

    @Test
    void evaluateShouldFailForMissingNode() {
        JsonNode node = mapper.missingNode();
        var result = fn.evaluate(node, Map.of("type", "camel"));
        assertFalse(result.isEmpty());
    }
}
