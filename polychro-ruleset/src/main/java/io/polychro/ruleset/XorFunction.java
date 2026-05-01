package io.polychro.ruleset;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * XOR function — asserts that exactly one of the specified properties is present.
 * <p>
 * Options:
 * <ul>
 *     <li>{@code properties} — list of property names (exactly one must exist)</li>
 * </ul>
 */
class XorFunction implements RuleFunction {

    @Override
    public String name() {
        return "xor";
    }

    @Override
    public List<String> evaluate(JsonNode targetNode, Map<String, Object> options) {
        if (targetNode == null || targetNode.isNull() || targetNode.isMissingNode()) {
            return List.of("Value must be an object for xor check");
        }
        if (!targetNode.isObject()) {
            return List.of("Value must be an object for xor check");
        }

        Object propsObj = options.get("properties");
        if (!(propsObj instanceof List<?> properties)) {
            return List.of("XOR function requires a 'properties' option");
        }

        int count = 0;
        for (Object prop : properties) {
            if (prop != null && targetNode.has(prop.toString())) {
                JsonNode field = targetNode.get(prop.toString());
                if (!field.isNull()) {
                    count++;
                }
            }
        }

        if (count == 1) {
            return List.of();
        }

        if (count == 0) {
            return List.of("Exactly one of " + properties + " must be present, but none were found");
        }

        return List.of("Exactly one of " + properties + " must be present, but " + count + " were found");
    }
}
