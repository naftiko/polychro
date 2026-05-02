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

            // Merge parent rules (earlier parents have lower priority)
            mergedRules.putAll(composedParent.rules());
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
                ruleset.functionsDir(),
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
}
