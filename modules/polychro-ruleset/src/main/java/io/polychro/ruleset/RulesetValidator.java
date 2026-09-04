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

import io.polychro.spi.Diagnostic;
import io.polychro.spi.Document;
import io.polychro.spi.Formats;
import io.polychro.spi.Validator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link Validator} that evaluates a Spectral-format ruleset against a document.
 * <p>
 * Parses the ruleset, then for each recommended (or explicitly enabled) rule,
 * executes it via {@link RuleExecutor} and collects diagnostics.
 */
class RulesetValidator implements Validator {

    private final Ruleset ruleset;
    private final RuleExecutor executor;
    private final boolean includeNonRecommended;
    private final AliasResolver aliasResolver;
    private final OverrideResolver overrideResolver;

    /**
     * Constructs a validator that resolves {@code functionsDir} relative to {@code baseDir}.
     *
     * <p>When the ruleset is loaded from a file, {@code baseDir} must be the ruleset file's
     * parent directory so that a relative {@code functionsDir} (e.g. {@code ./functions}) is
     * resolved against that directory rather than the process CWD (issue #44).
     * Pass {@code null} for {@code baseDir} when the ruleset comes from inline content — the
     * CWD-relative fallback is preserved in that case.
     *
     * @param ruleset              the composed ruleset to evaluate
     * @param includeNonRecommended whether to enable non-recommended rules
     */
    RulesetValidator(Ruleset ruleset, boolean includeNonRecommended) {
        this.ruleset = ruleset;
        FunctionRegistry functions = FunctionRegistry.forRuleset(ruleset.functions());
        this.executor = new RuleExecutor(new JsonPathEvaluator(), functions);
        this.includeNonRecommended = includeNonRecommended;
        this.aliasResolver = new AliasResolver();
        this.overrideResolver = new OverrideResolver();
    }

    @Override
    public String name() {
        return "ruleset";
    }

    @Override
    public List<Diagnostic> validate(Document doc) {
        List<Diagnostic> diagnostics = new ArrayList<>();

        // Collect effective aliases (base + override-scoped)
        Map<String, String> effectiveAliases = new LinkedHashMap<>(ruleset.aliases());
        if (doc.sourcePath() != null) {
            effectiveAliases.putAll(
                    overrideResolver.collectOverrideAliases(ruleset.overrides(), doc.sourcePath()));
        }

        // Determine effective rules (base + overrides)
        Map<String, Rule> effectiveRules = ruleset.rules();
        if (doc.sourcePath() != null) {
            effectiveRules = overrideResolver.applyOverrides(
                    effectiveRules, ruleset.overrides(), doc.sourcePath());
        }

        for (Rule rule : effectiveRules.values()) {
            if ("off".equalsIgnoreCase(rule.severity())) {
                continue;
            }
            if (!rule.recommended() && !includeNonRecommended) {
                continue;
            }
            if (!matchesFormat(rule, doc)) {
                continue;
            }

            // Resolve aliases in given expressions
            Rule resolvedRule = rule;
            if (!effectiveAliases.isEmpty() && !rule.given().isEmpty()) {
                List<String> resolvedGiven = aliasResolver.resolve(rule.given(), effectiveAliases);
                resolvedRule = new Rule(rule.name(), rule.message(), rule.description(),
                        rule.severity(), rule.recommended(), rule.formats(),
                        rule.documentationUrl(), resolvedGiven, rule.then());
            }

            diagnostics.addAll(executor.execute(resolvedRule, doc));
        }

        diagnostics.sort(null);
        return diagnostics;
    }

    /**
     * Determines whether {@code rule} applies to {@code doc}, matching against both the
     * document's syntax-level format ({@link Document#format()}, e.g. {@code yaml}/{@code json})
     * and, when present, its spec-level formats ({@code oas2}/{@code oas3}/{@code aas2}/
     * {@code aas3}) detected by {@link io.polychro.spi.SpecFormats} and carried under
     * {@link Document#SPEC_FORMATS_METADATA_KEY} — so a rule scoped with {@code formats: [oas3]}
     * matches an OpenAPI v3 document exactly as it would in Spectral (issue #83).
     *
     * <p>A ruleset-level {@code formats:} restriction ({@link Ruleset#formats()}) is enforced
     * first and applies to every rule in the ruleset that does not declare its own {@code
     * formats}, mirroring Spectral's top-level ruleset {@code formats} gate — this is how a
     * ruleset such as {@code polychro:openapi} restricts its unscoped shared rules (e.g.
     * {@code info-contact}) to OAS2/OAS3 documents without repeating {@code formats:} on every
     * rule.
     *
     * <p>An omitted rule-level {@code formats} ({@code rule.formats() == null}) is distinct from
     * an explicit {@code formats: []}: the former inherits the ruleset's own {@code formats}
     * (or matches everything when the ruleset has none), while the latter is a deliberate,
     * non-null empty set that matches <strong>no</strong> document — mirroring Spectral, which
     * preserves this distinction rather than treating an empty array as "inherit".
     *
     * <p>The same null-vs-empty distinction applies one level up, to the ruleset's own
     * {@link Ruleset#formats()}: an omitted top-level {@code formats:} ({@code null}) leaves
     * every unscoped rule format-agnostic (matches every document), while an explicit
     * {@code formats: []} restricts every unscoped rule to match <strong>no</strong> document —
     * collapsing both to an empty list would silently run rules that Spectral disables.
     */
    private boolean matchesFormat(Rule rule, Document doc) {
        List<String> effectiveFormats;
        if (rule.formats() == null) {
            if (ruleset.formats() == null) {
                return true;
            }
            effectiveFormats = ruleset.formats();
        } else if (rule.formats().isEmpty()) {
            return false;
        } else {
            effectiveFormats = rule.formats();
        }

        if (effectiveFormats.isEmpty()) {
            return false;
        }

        List<String> normalizedFormats = effectiveFormats.stream().map(Formats::normalize).toList();

        if (doc.format() != null && normalizedFormats.contains(Formats.normalize(doc.format()))) {
            return true;
        }

        return specFormats(doc).stream().anyMatch(normalizedFormats::contains);
    }

    @SuppressWarnings("unchecked")
    private static List<String> specFormats(Document doc) {
        Object specFormats = doc.metadata().get(Document.SPEC_FORMATS_METADATA_KEY);
        return specFormats instanceof List<?> list ? (List<String>) list : List.of();
    }
}
