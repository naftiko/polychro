package io.polychro.ruleset;

import com.fasterxml.jackson.databind.JsonNode;
import io.polychro.spi.Diagnostic;
import io.polychro.spi.Document;
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

    RulesetValidator(Ruleset ruleset, boolean includeNonRecommended) {
        this.ruleset = ruleset;
        this.executor = new RuleExecutor(new JsonPathEvaluator());
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
        JsonNode root = doc.root();
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

            // Resolve aliases in given expressions
            Rule resolvedRule = rule;
            if (!effectiveAliases.isEmpty() && !rule.given().isEmpty()) {
                List<String> resolvedGiven = aliasResolver.resolve(rule.given(), effectiveAliases);
                resolvedRule = new Rule(rule.name(), rule.message(), rule.description(),
                        rule.severity(), rule.recommended(), rule.formats(),
                        rule.documentationUrl(), resolvedGiven, rule.then());
            }

            diagnostics.addAll(executor.execute(resolvedRule, root));
        }

        diagnostics.sort(null);
        return diagnostics;
    }
}
