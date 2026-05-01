package io.polychro.ruleset;

import java.util.List;
import java.util.Map;

/**
 * A parsed Spectral-format ruleset.
 *
 * @param extendsRefs     list of base ruleset paths to inherit from
 * @param aliases         global alias map (alias name → JSONPath expression)
 * @param formats         list of document format identifiers this ruleset applies to
 * @param functionsDir    directory path for custom function files
 * @param functions       list of custom function names declared
 * @param rules           rule definitions keyed by rule name
 * @param documentationUrl base URL for rule documentation
 */
record Ruleset(
        List<String> extendsRefs,
        Map<String, String> aliases,
        List<String> formats,
        String functionsDir,
        List<String> functions,
        Map<String, Rule> rules,
        String documentationUrl
) {
    Ruleset {
        extendsRefs = extendsRefs != null ? List.copyOf(extendsRefs) : List.of();
        aliases = aliases != null ? Map.copyOf(aliases) : Map.of();
        formats = formats != null ? List.copyOf(formats) : List.of();
        functions = functions != null ? List.copyOf(functions) : List.of();
        rules = rules != null ? Map.copyOf(rules) : Map.of();
    }
}
