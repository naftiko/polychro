package io.polychro.ruleset;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * Length function — asserts the target value's length is within bounds.
 * <p>
 * Works for strings (character count), arrays (element count), and objects (key count).
 * <p>
 * Options:
 * <ul>
 *     <li>{@code min} — minimum length (inclusive)</li>
 *     <li>{@code max} — maximum length (inclusive)</li>
 * </ul>
 */
class LengthFunction implements RuleFunction {

    @Override
    public String name() {
        return "length";
    }

    @Override
    public List<String> evaluate(JsonNode targetNode, Map<String, Object> options) {
        if (targetNode == null || targetNode.isNull() || targetNode.isMissingNode()) {
            return List.of("Value must not be null for length check");
        }

        int length = resolveLength(targetNode);
        if (length < 0) {
            return List.of("Value must be a string, array, or object for length check");
        }

        Integer min = intOption(options, "min");
        Integer max = intOption(options, "max");

        if (min != null && length < min) {
            return List.of("Length " + length + " is less than minimum " + min);
        }
        if (max != null && length > max) {
            return List.of("Length " + length + " is greater than maximum " + max);
        }

        return List.of();
    }

    private int resolveLength(JsonNode node) {
        if (node.isTextual()) {
            return node.asText().length();
        }
        if (node.isArray()) {
            return node.size();
        }
        if (node.isObject()) {
            return node.size();
        }
        return -1;
    }

    private Integer intOption(Map<String, Object> options, String key) {
        Object val = options.get(key);
        if (val == null) {
            return null;
        }
        if (val instanceof Number num) {
            return num.intValue();
        }
        try {
            return Integer.parseInt(val.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
