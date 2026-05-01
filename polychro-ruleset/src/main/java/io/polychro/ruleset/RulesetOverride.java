package io.polychro.ruleset;

import java.util.List;
import java.util.Map;

/**
 * A single override block within a ruleset.
 * <p>
 * Overrides allow file-scoped rule customizations: enable/disable rules,
 * change severity, or add rules per file glob pattern. Optionally,
 * a JSON Pointer can target specific elements within matched files.
 *
 * @param files           list of file glob patterns (Ant-style) with optional JSON Pointer (e.g., "**#/paths")
 * @param rules           rule overrides — map of rule name to rule definition (may override severity, enable/disable)
 * @param aliases         aliases scoped to this override block
 * @param formats         format restrictions for this override block
 */
record RulesetOverride(
        List<String> files,
        Map<String, Rule> rules,
        Map<String, String> aliases,
        List<String> formats
) {
    RulesetOverride {
        files = files != null ? List.copyOf(files) : List.of();
        rules = rules != null ? Map.copyOf(rules) : Map.of();
        aliases = aliases != null ? Map.copyOf(aliases) : Map.of();
        formats = formats != null ? List.copyOf(formats) : null;
    }
}
