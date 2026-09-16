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

import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Resolves ruleset inheritance ({@code extends}) by loading base rulesets,
 * merging rules, and detecting circular references.
 */
class RulesetComposer {

    /**
     * Classpath base under which bundled rulesets are packaged (mirrors
     * {@code io.polychro.rulesets.RulesetCatalog}'s own {@code BASE} — this module cannot depend
     * on {@code polychro-rulesets}, which depends on this one, so the base path is duplicated
     * here rather than shared).
     */
    private static final String BUNDLED_CLASSPATH_BASE = "/rulesets";

    /**
     * Maps a Spectral built-in bundle reference ({@code extends: spectral:oas}) to the bundled
     * Polychro ruleset that stands in for it (naftiko/polychro#84 — Phase 3 of the Spectral
     * iso-functionality track, naftiko/polychro#76). This is a direct classpath resolution — the
     * same mechanism {@code RulesetParser.RulesetSource.CLASSPATH} already uses for bundled
     * rulesets loaded via {@code RulesetCatalog} — rather than a new resolution SPI, because the
     * whole ruleset-composition mechanism is due for a rework in a later story; growing a second,
     * parallel bundled-ruleset mechanism now would only add debt for that rework to absorb. A
     * {@code polychro:<name>} ref (this module's own bundled rulesets) is resolved directly from
     * its name, without a lookup table.
     */
    private static final Map<String, String> BUNDLED_ALIASES = Map.of(
            "spectral:oas", "openapi"
    );

    private static final String SPECTRAL_PREFIX = "spectral:";
    private static final String POLYCHRO_PREFIX = "polychro:";

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
            boolean bundled = isBundledRef(ref);
            // A bundled ref (e.g. "spectral:oas") has no filesystem location to resolve against,
            // so its cycle-detection key is the literal ref string rather than a path resolved
            // relative to baseDir.
            String normalizedRef = bundled ? ref : normalizeRef(ref, baseDir);
            if (visited.contains(normalizedRef)) {
                List<String> cycle = new ArrayList<>(visited);
                cycle.add(normalizedRef);
                throw new RulesetParseException(
                        "Circular extends detected: " + String.join(" -> ", cycle));
            }

            Set<String> newVisited = new LinkedHashSet<>(visited);
            newVisited.add(normalizedRef);

            Ruleset parent = bundled ? loadBundledRef(ref) : loadParent(ref, baseDir);
            // A bundled ruleset carries no filesystem-relative extends of its own today, so the
            // parent directory used to resolve any FURTHER extends inside it is irrelevant; fall
            // back to the current baseDir rather than null.
            Path parentDir = bundled ? baseDir : resolveParentDir(ref, baseDir);
            Ruleset composedParent = compose(parent, parentDir, newVisited);

            // Merge parent rules (earlier parents have lower priority). Rules that don't declare
            // their own `formats` are implicitly gated by the parent ruleset's top-level `formats`
            // (RulesetValidator.matchesFormat's fallback) — but once flattened into mergedRules,
            // that ownership is lost: the final composed ruleset below carries only the CHILD's
            // own `formats()`, so if the child has none, an inherited unscoped rule would run
            // against every document instead of staying gated by the parent it came from. Stamp
            // the parent's effective top-level formats onto its own unscoped rules before merging
            // so the gate survives composition, mirroring Spectral's inherited-ruleset formats.
            Map<String, Rule> parentRules = withInheritedFormats(composedParent.rules(), composedParent.formats());

            // naftiko/polychro#84 — an explicit tuple-form severity marker on this extends entry
            // (`["ref", "off"|"recommended"|"all"]`) remaps the FULL set of rules this parent
            // contributes, mirroring Spectral's `Rule.isEnabled` (verified against
            // stoplightio/spectral source): it overrides what the rules' own `recommended`/
            // `severity` would otherwise produce, regardless of the validator's own
            // `includeNonRecommended` setting. A bare-string ref (no tuple) carries no entry in
            // `extendsSeverities()` and the rules pass through unchanged — today's behavior.
            String severity = ruleset.extendsSeverities().get(ref);
            if (severity != null) {
                parentRules = applyExtendsSeverity(parentRules, severity);
            }

            mergedRules.putAll(parentRules);
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

    private boolean isBundledRef(String ref) {
        return ref.startsWith(SPECTRAL_PREFIX) || ref.startsWith(POLYCHRO_PREFIX);
    }

    /**
     * Resolves a bundled ruleset reference (naftiko/polychro#84) — {@code spectral:*} through
     * {@link #BUNDLED_ALIASES}, {@code polychro:*} directly by name — and loads it from the
     * classpath via {@link RulesetParser.RulesetSource#CLASSPATH}, the same mechanism
     * {@code io.polychro.rulesets.RulesetCatalog} uses to load its own bundled rulesets.
     *
     * @throws RulesetParseException if {@code ref} is an unrecognized {@code spectral:*} bundle,
     *                                or the resolved bundled ruleset resource cannot be read
     */
    private Ruleset loadBundledRef(String ref) {
        String name;
        if (ref.startsWith(SPECTRAL_PREFIX)) {
            name = BUNDLED_ALIASES.get(ref);
            if (name == null) {
                throw new RulesetParseException(
                        "Unknown Spectral bundle reference: '" + ref + "'. Bundled equivalents: "
                                + BUNDLED_ALIASES.keySet());
            }
        } else {
            name = ref.substring(POLYCHRO_PREFIX.length());
        }

        String resource = BUNDLED_CLASSPATH_BASE + "/" + name + ".yml";
        try {
            return parser.parse(resource, BUNDLED_CLASSPATH_BASE, RulesetParser.RulesetSource.CLASSPATH);
        } catch (UncheckedIOException e) {
            throw new RulesetParseException(
                    "Bundled ruleset not found for '" + ref + "' (resolved to classpath:" + resource + ")");
        }
    }

    /**
     * Remaps the effective severity/enablement of every rule in {@code rules}, per the tuple-form
     * {@code extends} severity marker (naftiko/polychro#84): {@code off} disables every rule,
     * {@code recommended} disables only the ones whose own {@code recommended} flag is false,
     * and {@code all} force-enables every rule (raising an explicit {@code off} to {@code warn}
     * and forcing {@code recommended} to {@code true}) regardless of its own declared state. An
     * unrecognized marker is a defensive no-op — a malformed marker cannot reach here in practice
     * since {@link RulesetParser#parseExtendsSeverities} only ever stores the literal string
     * found in the YAML tuple, but the ruleset must not fail loading over it.
     */
    private Map<String, Rule> applyExtendsSeverity(Map<String, Rule> rules, String severity) {
        return switch (severity.toLowerCase(Locale.ROOT)) {
            case "off" -> mapRules(rules, rule -> withSeverity(rule, "off"));
            case "recommended" -> mapRules(rules, rule -> rule.recommended() ? rule : withSeverity(rule, "off"));
            case "all" -> mapRules(rules, this::forceEnabled);
            default -> rules;
        };
    }

    private Rule forceEnabled(Rule rule) {
        Rule recommended = rule.recommended() ? rule : withRecommended(rule, true);
        return "off".equalsIgnoreCase(recommended.severity()) ? withSeverity(recommended, "warn") : recommended;
    }

    private Map<String, Rule> mapRules(Map<String, Rule> rules, java.util.function.UnaryOperator<Rule> remap) {
        Map<String, Rule> result = new LinkedHashMap<>();
        for (Map.Entry<String, Rule> entry : rules.entrySet()) {
            result.put(entry.getKey(), remap.apply(entry.getValue()));
        }
        return result;
    }

    private Rule withSeverity(Rule rule, String severity) {
        return new Rule(rule.name(), rule.message(), rule.description(), severity, rule.recommended(),
                rule.formats(), rule.documentationUrl(), rule.given(), rule.then());
    }

    private Rule withRecommended(Rule rule, boolean recommended) {
        return new Rule(rule.name(), rule.message(), rule.description(), rule.severity(), recommended,
                rule.formats(), rule.documentationUrl(), rule.given(), rule.then());
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
