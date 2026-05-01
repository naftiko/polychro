package io.polychro.ruleset;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * Interface for ruleset functions that evaluate a node against configured options.
 */
public interface RuleFunction {

    /**
     * @return the function name as used in ruleset YAML (e.g. "truthy", "pattern")
     */
    String name();

    /**
     * Evaluate the given target node.
     *
     * @param targetNode the resolved JSON node to evaluate (may be null if field is missing)
     * @param options    function-specific options from the rule action
     * @return list of error messages; empty if the node passes
     */
    List<String> evaluate(JsonNode targetNode, Map<String, Object> options);
}
