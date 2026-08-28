/**
 * Copyright 2026 Naftiko
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.polychro.ruleset;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resolves ruleset inheritance ({@code extends}) by loading base rulesets,
 * merging rules, and detecting circular references.
 */
class RulesetComposer {

    private final RulesetParser parser;

    RulesetComposer(RulesetParser parser) {
        this.parser = parser;
    }

    /**
     * Resolve extends for a ruleset, loading parent rulesets from paths relative to the
     * given base directory, merging rules (child overrides parent), and detecting cycles.
     *
     * @param ruleset the child ruleset to resolve
     * @param baseDir the base directory for resolving relative extends paths
     * @return a fully composed ruleset with all inherited rules merged
     * @throws RulesetParseException if a circular extends is detected or a referenced file is invalid
     */
    Ruleset compose(Ruleset ruleset, Path baseDir) {
        LinkedHashSet<String> visited = new LinkedHashSet<>();
        // If called from a file-based parse, we don't know the source path here
        return compose(ruleset, baseDir, visited);
    }

    Ruleset compose(Ruleset ruleset, Path baseDir, String sourcePath) {
        LinkedHashSet<String> visited = new LinkedHashSet<>();
        if (sourcePath != null) {
            visited.add(Path.of(sourcePath).normalize().toString());
        }
        return compose(ruleset, baseDir, visited);
    }

    /**
     * Resolve extends for a ruleset parsed from a string (no base directory).
     * Extends references that are relative paths will be resolved from the current working directory.
     *
     * @param ruleset the child ruleset to resolve
     * @return a fully composed ruleset with all inherited rules merged
     * @throws RulesetParseException if a circular extends is detected
     */
    Ruleset compose(Ruleset ruleset) {
        return compose(ruleset, Path.of("."), new LinkedHashSet<>());
    }

    private Ruleset compose(Ruleset ruleset, Path baseDir, Set<String> visited) {
        if (ruleset.extendsRefs().isEmpty()) {
            return ruleset;
        }

        Map<String, Rule> mergedRules = new LinkedHashMap<>();

        for (String ref : ruleset.extendsRefs()) {
            String normalizedRef = normalizeRef(ref, baseDir);
            if (visited.contains(normalizedRef)) {
                List<String> cycle = new ArrayList<>(visited);
                cycle.add(normalizedRef);
                throw new RulesetParseException(
                        "Circular extends detected: " + String.join(" -> ", cycle));
            }

            Set<String> newVisited = new LinkedHashSet<>(visited);
            newVisited.add(normalizedRef);

            Ruleset parent = loadParent(ref, baseDir);
            Path parentDir = resolveParentDir(ref, baseDir);
            Ruleset composedParent = compose(parent, parentDir, newVisited);

            // Merge parent rules (earlier parents have lower priority). Rules that don't declare
            // their own `formats` are implicitly gated by the parent ruleset's top-level `formats`
            // (RulesetValidator.matchesFormat's fallback) — but once flattened into mergedRules,
            // that ownership is lost: the final composed ruleset below carries only the CHILD's
            // own `formats()`, so if the child has none, an inherited unscoped rule would run
            // against every document instead of staying gated by the parent it came from. Stamp
            // the parent's effective top-level formats onto its own unscoped rules before merging
            // so the gate survives composition, mirroring Spectral's inherited-ruleset formats.
            mergedRules.putAll(withInheritedFormats(composedParent.rules(), composedParent.formats()));
        }

        // Child rules override parent rules by name
        for (Map.Entry<String, Rule> entry : ruleset.rules().entrySet()) {
            String name = entry.getKey();
            Rule childRule = entry.getValue();

            if ("off".equalsIgnoreCase(childRule.severity()) && childRule.given().isEmpty()
                    && childRule.then().isEmpty()) {
                // Bare "off" — disable inherited rule
                mergedRules.put(name, new Rule(name, null, null, "off", false,
                        null, null, List.of(), List.of()));
            } else {
                mergedRules.put(name, childRule);
            }
        }

        return new Ruleset(
                List.of(), // extends already resolved
                ruleset.aliases(),
                ruleset.overrides(),
                ruleset.formats(),
                ruleset.functions(),
                mergedRules,
                ruleset.documentationUrl()
        );
    }

    private Ruleset loadParent(String ref, Path baseDir) {
        Path parentPath = baseDir.resolve(ref).normalize();
        return parser.parse(parentPath);
    }

    private Path resolveParentDir(String ref, Path baseDir) {
        Path parentPath = baseDir.resolve(ref).toAbsolutePath().normalize();
        return parentPath.getParent();
    }

    private String normalizeRef(String ref, Path baseDir) {
        return baseDir.resolve(ref).normalize().toString();
    }

    /**
     * Stamps {@code parentFormats} onto every rule in {@code rules} that omits its own
     * {@code formats} (null), so the parent ruleset's top-level format gate survives being
     * flattened into the composed ruleset's rule map.
     *
     * <p>{@code parentFormats} itself distinguishes an omitted parent-level {@code formats:}
     * ({@code null} — the parent is format-agnostic, so its unscoped rules need no stamping and
     * keep matching every document) from an explicit {@code formats: []} (a deliberate
     * deny-all that must still be stamped, otherwise the composed ruleset's own top-level
     * {@code formats} — which may be {@code null} or a different restriction — would silently
     * let the inherited rule match again).
     */
    private Map<String, Rule> withInheritedFormats(Map<String, Rule> rules, List<String> parentFormats) {
        if (parentFormats == null) {
            return rules;
        }
        Map<String, Rule> result = new LinkedHashMap<>();
        for (Map.Entry<String, Rule> entry : rules.entrySet()) {
            Rule rule = entry.getValue();
            // Only an OMITTED rule-level `formats` (null) inherits the parent ruleset's formats.
            // An explicit `formats: []` is a deliberate, non-null empty set that must keep
            // matching no document — mirroring Spectral, which preserves this distinction
            // instead of treating an empty array the same as "not declared".
            if (rule.formats() == null) {
                rule = new Rule(rule.name(), rule.message(), rule.description(), rule.severity(),
                        rule.recommended(), parentFormats, rule.documentationUrl(), rule.given(),
                        rule.then());
            }
            result.put(entry.getKey(), rule);
        }
        return result;
    }
}
