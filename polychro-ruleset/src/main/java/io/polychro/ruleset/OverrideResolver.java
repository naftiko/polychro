package io.polychro.ruleset;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves file-glob-scoped overrides for a given document path.
 * <p>
 * Overrides can:
 * <ul>
 *   <li>Enable or disable rules per file pattern</li>
 *   <li>Change rule severity per file pattern</li>
 *   <li>Add rules that only apply to certain files</li>
 *   <li>Target specific JSON Pointer elements within matched files</li>
 *   <li>Scope aliases and formats to the override block</li>
 * </ul>
 */
class OverrideResolver {

    /**
     * Apply overrides to the base rules for a specific document path.
     * Returns a new rules map with overrides applied (last-wins priority).
     *
     * @param baseRules    the original rules map
     * @param overrides    the list of override blocks
     * @param documentPath the path of the document being validated
     * @return the final rules map with overrides applied
     */
    Map<String, Rule> applyOverrides(Map<String, Rule> baseRules, List<RulesetOverride> overrides,
                                     String documentPath) {
        if (overrides == null || overrides.isEmpty()) {
            return baseRules;
        }

        Map<String, Rule> result = new LinkedHashMap<>(baseRules);

        for (RulesetOverride override : overrides) {
            if (matchesDocument(override.files(), documentPath)) {
                // Apply rule overrides — last wins
                for (Map.Entry<String, Rule> entry : override.rules().entrySet()) {
                    result.put(entry.getKey(), entry.getValue());
                }
            }
        }

        return result;
    }

    /**
     * Collect aliases from matching override blocks for a given document path.
     *
     * @param overrides    the list of override blocks
     * @param documentPath the path of the document being validated
     * @return merged aliases from matching override blocks
     */
    Map<String, String> collectOverrideAliases(List<RulesetOverride> overrides, String documentPath) {
        if (overrides == null || overrides.isEmpty()) {
            return Map.of();
        }

        Map<String, String> aliases = new LinkedHashMap<>();
        for (RulesetOverride override : overrides) {
            if (matchesDocument(override.files(), documentPath)) {
                aliases.putAll(override.aliases());
            }
        }
        return aliases;
    }

    /**
     * Get the applicable formats restriction from matching override blocks.
     *
     * @param overrides    the list of override blocks
     * @param documentPath the path of the document being validated
     * @return the last matching override's formats, or null if no format restriction
     */
    List<String> getOverrideFormats(List<RulesetOverride> overrides, String documentPath) {
        if (overrides == null || overrides.isEmpty()) {
            return null;
        }

        List<String> formats = null;
        for (RulesetOverride override : overrides) {
            if (matchesDocument(override.files(), documentPath) && override.formats() != null) {
                formats = override.formats();
            }
        }
        return formats;
    }

    /**
     * Extract JSON Pointer from a file pattern, if present.
     * File patterns can include a JSON Pointer suffix: "schemas/**#/definitions/Pet"
     *
     * @param filePattern the file pattern to extract from
     * @return the JSON Pointer, or null if not present
     */
    String extractJsonPointer(String filePattern) {
        int pointerIdx = filePattern.indexOf('#');
        if (pointerIdx >= 0 && pointerIdx < filePattern.length() - 1) {
            String pointer = filePattern.substring(pointerIdx + 1);
            if (pointer.startsWith("/")) {
                return pointer;
            }
        }
        return null;
    }

    /**
     * Extract the glob portion from a file pattern (before the JSON Pointer).
     *
     * @param filePattern the file pattern
     * @return the glob portion
     */
    String extractGlob(String filePattern) {
        int pointerIdx = filePattern.indexOf('#');
        if (pointerIdx >= 0) {
            return filePattern.substring(0, pointerIdx);
        }
        return filePattern;
    }

    /**
     * Check if any of the file patterns match the given document path.
     * Supports Ant-style glob patterns and optional JSON Pointer suffixes.
     */
    boolean matchesDocument(List<String> filePatterns, String documentPath) {
        if (filePatterns == null || filePatterns.isEmpty() || documentPath == null) {
            return false;
        }

        String normalizedPath = documentPath.replace('\\', '/');

        for (String pattern : filePatterns) {
            String glob = extractGlob(pattern);
            if (matchesGlob(glob, normalizedPath)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesGlob(String glob, String path) {
        if (glob.isEmpty()) {
            return true; // No glob restriction — match all
        }

        // Normalize both glob and path to forward slashes for cross-platform matching
        String normalizedGlob = glob.replace('\\', '/');
        String normalizedPath = path.replace('\\', '/');

        // Simple regex-based glob matching to avoid platform-specific PathMatcher behavior
        String regex = globToRegex(normalizedGlob);
        return normalizedPath.matches(regex);
    }

    private String globToRegex(String glob) {
        StringBuilder regex = new StringBuilder();
        int i = 0;
        while (i < glob.length()) {
            char c = glob.charAt(i);
            if (c == '*') {
                if (i + 1 < glob.length() && glob.charAt(i + 1) == '*') {
                    // ** matches any path segment(s)
                    if (i + 2 < glob.length() && glob.charAt(i + 2) == '/') {
                        regex.append("(.*/)?");
                        i += 3;
                    } else {
                        regex.append(".*");
                        i += 2;
                    }
                } else {
                    // * matches anything except /
                    regex.append("[^/]*");
                    i++;
                }
            } else if (c == '?') {
                regex.append("[^/]");
                i++;
            } else if (c == '.') {
                regex.append("\\.");
                i++;
            } else {
                regex.append(c);
                i++;
            }
        }
        return regex.toString();
    }
}
