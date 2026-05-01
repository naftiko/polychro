package io.polychro.ruleset;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * Enumeration function — asserts the target value is one of the allowed values.
 * <p>
 * Options:
 * <ul>
 *     <li>{@code values} — list of allowed values</li>
 * </ul>
 */
class EnumerationFunction implements RuleFunction {

    @Override
    public String name() {
        return "enumeration";
    }

    @Override
    public List<String> evaluate(JsonNode targetNode, Map<String, Object> options) {
        if (targetNode == null || targetNode.isNull() || targetNode.isMissingNode()) {
            return List.of("Value must not be null");
        }

        Object valuesObj = options.get("values");
        if (!(valuesObj instanceof List<?> allowedValues)) {
            return List.of("Enumeration function requires a 'values' option");
        }

        if (allowedValues.isEmpty()) {
            return List.of("Value is not in the allowed list");
        }

        String targetValue = targetNode.asText();
        for (Object allowed : allowedValues) {
            if (allowed != null && allowed.toString().equals(targetValue)) {
                return List.of();
            }
        }

        return List.of("Value \"" + targetValue + "\" is not in the allowed list");
    }
}
