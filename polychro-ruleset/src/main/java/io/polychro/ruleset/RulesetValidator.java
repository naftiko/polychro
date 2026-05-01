package io.polychro.ruleset;

import com.fasterxml.jackson.databind.JsonNode;
import io.polychro.spi.Diagnostic;
import io.polychro.spi.Document;
import io.polychro.spi.Validator;

import java.util.ArrayList;
import java.util.List;

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

    RulesetValidator(Ruleset ruleset, boolean includeNonRecommended) {
        this.ruleset = ruleset;
        this.executor = new RuleExecutor(new JsonPathEvaluator());
        this.includeNonRecommended = includeNonRecommended;
    }

    @Override
    public String name() {
        return "ruleset";
    }

    @Override
    public List<Diagnostic> validate(Document doc) {
        JsonNode root = doc.root();
        List<Diagnostic> diagnostics = new ArrayList<>();

        for (Rule rule : ruleset.rules().values()) {
            if ("off".equalsIgnoreCase(rule.severity())) {
                continue;
            }
            if (!rule.recommended() && !includeNonRecommended) {
                continue;
            }
            diagnostics.addAll(executor.execute(rule, root));
        }

        diagnostics.sort(null);
        return diagnostics;
    }
}
