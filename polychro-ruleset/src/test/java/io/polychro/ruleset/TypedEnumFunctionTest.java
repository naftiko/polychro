package io.polychro.ruleset;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TypedEnumFunctionTest {

    private final TypedEnumFunction fn = new TypedEnumFunction();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void nameShouldReturnTypedEnum() {
        assertEquals("typedEnum", fn.name());
    }

    @Test
    void evaluateShouldPassForStringEnum() {
        JsonNode node = mapper.valueToTree("active");
        var result = fn.evaluate(node, Map.of("type", "string", "values", List.of("active", "inactive")));
        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForStringEnumMismatch() {
        JsonNode node = mapper.valueToTree("unknown");
        var result = fn.evaluate(node, Map.of("type", "string", "values", List.of("active", "inactive")));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldPassForIntegerEnum() {
        JsonNode node = mapper.valueToTree(1);
        var result = fn.evaluate(node, Map.of("type", "integer", "values", List.of(1, 2, 3)));
        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForIntegerEnumMismatch() {
        JsonNode node = mapper.valueToTree(5);
        var result = fn.evaluate(node, Map.of("type", "integer", "values", List.of(1, 2, 3)));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForTypeCoercionMismatch() {
        // String "1" should not match integer type
        JsonNode node = mapper.valueToTree("1");
        var result = fn.evaluate(node, Map.of("type", "integer", "values", List.of(1)));
        assertFalse(result.isEmpty());
        assertTrue(result.get(0).contains("type does not match"));
    }

    @Test
    void evaluateShouldPassForBooleanEnum() {
        JsonNode node = mapper.valueToTree(true);
        var result = fn.evaluate(node, Map.of("type", "boolean", "values", List.of(true)));
        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForBooleanEnumMismatch() {
        JsonNode node = mapper.valueToTree(false);
        var result = fn.evaluate(node, Map.of("type", "boolean", "values", List.of(true)));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldPassForNumberEnum() {
        JsonNode node = mapper.valueToTree(3.14);
        var result = fn.evaluate(node, Map.of("type", "number", "values", List.of(3.14, 2.71)));
        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForNullNode() {
        var result = fn.evaluate(null, Map.of("type", "string", "values", List.of("a")));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForJsonNull() {
        JsonNode node = mapper.nullNode();
        var result = fn.evaluate(node, Map.of("type", "string", "values", List.of("a")));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldFailWhenNoTypeOption() {
        JsonNode node = mapper.valueToTree("test");
        var result = fn.evaluate(node, Map.of("values", List.of("test")));
        assertFalse(result.isEmpty());
        assertTrue(result.get(0).contains("'type' option"));
    }

    @Test
    void evaluateShouldFailWhenNoValuesOption() {
        JsonNode node = mapper.valueToTree("test");
        var result = fn.evaluate(node, Map.of("type", "string"));
        assertFalse(result.isEmpty());
        assertTrue(result.get(0).contains("'values' option"));
    }

    @Test
    void evaluateShouldFailForUnknownType() {
        JsonNode node = mapper.valueToTree("test");
        var result = fn.evaluate(node, Map.of("type", "unknown", "values", List.of("test")));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForMissingNode() {
        JsonNode node = mapper.missingNode();
        var result = fn.evaluate(node, Map.of("type", "string", "values", List.of("a")));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldHandleNullAllowedValue() {
        JsonNode node = mapper.valueToTree("test");
        List<Object> values = new java.util.ArrayList<>();
        values.add(null);
        values.add("test");
        var result = fn.evaluate(node, Map.of("type", "string", "values", values));
        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForStringNotMatchingBooleanValues() {
        // Values list contains non-Boolean items for boolean type
        JsonNode node = mapper.valueToTree(true);
        var result = fn.evaluate(node, Map.of("type", "boolean", "values", List.of("true")));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForIntegerNotMatchingStringValues() {
        // Values list contains non-Number items for integer type
        JsonNode node = mapper.valueToTree(1);
        var result = fn.evaluate(node, Map.of("type", "integer", "values", List.of("1")));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForNumberNotMatchingStringValues() {
        // Values list contains non-Number items for number type
        JsonNode node = mapper.valueToTree(3.14);
        var result = fn.evaluate(node, Map.of("type", "number", "values", List.of("3.14")));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForNumberNotInNumericList() {
        JsonNode node = mapper.valueToTree(9.99);
        var result = fn.evaluate(node, Map.of("type", "number", "values", List.of(3.14, 2.71)));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForIntegerNotInIntegerList() {
        JsonNode node = mapper.valueToTree(99);
        var result = fn.evaluate(node, Map.of("type", "integer", "values", List.of(1, 2, 3)));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForBooleanNotInBooleanList() {
        JsonNode node = mapper.valueToTree(false);
        var result = fn.evaluate(node, Map.of("type", "boolean", "values", List.of(true)));
        assertFalse(result.isEmpty());
    }
}
