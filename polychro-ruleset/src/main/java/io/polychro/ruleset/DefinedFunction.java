package io.polychro.ruleset;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * Defined function — asserts the target node exists (is not null or missing).
 */
class DefinedFunction implements RuleFunction {

    @Override
    public String name() {
        return "defined";
    }

    @Override
    public List<String> evaluate(JsonNode targetNode, Map<String, Object> options) {
        if (targetNode == null || targetNode.isMissingNode()) {
            return List.of("Value must be defined");
        }
        return List.of();
    }
}
