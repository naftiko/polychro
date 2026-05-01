package io.polychro.ruleset;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * Undefined function — asserts the target node does NOT exist (is null or missing).
 */
class UndefinedFunction implements RuleFunction {

    @Override
    public String name() {
        return "undefined";
    }

    @Override
    public List<String> evaluate(JsonNode targetNode, Map<String, Object> options) {
        if (targetNode != null && !targetNode.isMissingNode()) {
            return List.of("Value must not be defined");
        }
        return List.of();
    }
}
