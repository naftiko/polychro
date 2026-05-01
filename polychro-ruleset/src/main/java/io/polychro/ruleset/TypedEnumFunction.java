package io.polychro.ruleset;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * TypedEnum function — asserts the target value matches a type-specific enumeration.
 * <p>
 * Unlike plain "enumeration", this function compares values using type-aware equality
 * (e.g. integer 1 does not match string "1").
 * <p>
 * Options:
 * <ul>
 *     <li>{@code type} — the expected type: string, number, integer, boolean</li>
 *     <li>{@code values} — list of allowed values (must match the type)</li>
 * </ul>
 */
class TypedEnumFunction implements RuleFunction {

    @Override
    public String name() {
        return "typedEnum";
    }

    @Override
    public List<String> evaluate(JsonNode targetNode, Map<String, Object> options) {
        if (targetNode == null || targetNode.isNull() || targetNode.isMissingNode()) {
            return List.of("Value must not be null");
        }

        Object typeObj = options.get("type");
        if (typeObj == null) {
            return List.of("TypedEnum function requires a 'type' option");
        }
        String expectedType = typeObj.toString();

        Object valuesObj = options.get("values");
        if (!(valuesObj instanceof List<?> allowedValues)) {
            return List.of("TypedEnum function requires a 'values' option");
        }

        if (!matchesType(targetNode, expectedType)) {
            return List.of("Value type does not match expected type: " + expectedType);
        }

        for (Object allowed : allowedValues) {
            if (valuesMatch(targetNode, allowed, expectedType)) {
                return List.of();
            }
        }

        return List.of("Value is not in the typed enum list");
    }

    private boolean matchesType(JsonNode node, String type) {
        return switch (type) {
            case "string" -> node.isTextual();
            case "number" -> node.isNumber();
            case "integer" -> node.isIntegralNumber();
            case "boolean" -> node.isBoolean();
            default -> false;
        };
    }

    private boolean valuesMatch(JsonNode node, Object allowed, String type) {
        if (allowed == null) {
            return false;
        }
        if ("string".equals(type)) {
            return node.asText().equals(allowed.toString());
        }
        if ("number".equals(type)) {
            return allowed instanceof Number num && node.numberValue().doubleValue() == num.doubleValue();
        }
        if ("integer".equals(type)) {
            return allowed instanceof Number num && node.asLong() == num.longValue();
        }
        // boolean — only remaining type that passes matchesType
        return allowed instanceof Boolean bool && node.asBoolean() == bool;
    }
}
