package io.polychro.ruleset;

import java.util.Map;

/**
 * An action to apply when a rule matches (the "then" clause).
 *
 * @param field           optional field path to target within the matched node
 * @param functionName    name of the function to invoke
 * @param functionOptions options passed to the function
 */
record RuleAction(
        String field,
        String functionName,
        Map<String, Object> functionOptions
) {
    RuleAction {
        functionOptions = functionOptions != null ? Map.copyOf(functionOptions) : Map.of();
    }
}
