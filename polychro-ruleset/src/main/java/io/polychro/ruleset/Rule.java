package io.polychro.ruleset;

import java.util.List;

/**
 * A single rule within a ruleset.
 *
 * @param name             rule identifier (map key in the ruleset YAML)
 * @param message          human-readable error message template
 * @param description      optional longer description
 * @param severity         error, warn, info, hint, or off
 * @param recommended      whether this rule is enabled by default
 * @param formats          document formats this rule applies to (null = all)
 * @param documentationUrl URL to rule documentation
 * @param given            list of JSONPath expressions or alias references
 * @param then             list of actions to apply at each matched node
 */
record Rule(
        String name,
        String message,
        String description,
        String severity,
        boolean recommended,
        List<String> formats,
        String documentationUrl,
        List<String> given,
        List<RuleAction> then
) {
    Rule {
        given = given != null ? List.copyOf(given) : List.of();
        then = then != null ? List.copyOf(then) : List.of();
        formats = formats != null ? List.copyOf(formats) : null;
        if (severity == null) {
            severity = "warn";
        }
    }
}
