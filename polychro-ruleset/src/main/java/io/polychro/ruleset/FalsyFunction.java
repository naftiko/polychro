package io.polychro.ruleset;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * Falsy function — asserts the target value is "falsy" (null, empty, zero, or false).
 */
class FalsyFunction implements RuleFunction {

    @Override
    public String name() {
        return "falsy";
    }

    @Override
    public List<String> evaluate(JsonNode targetNode, Map<String, Object> options) {
        if (!TruthyFunction.isFalsy(targetNode)) {
            return List.of("Value must be falsy");
        }
        return List.of();
    }
}
