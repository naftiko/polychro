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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import io.polychro.spi.Diagnostic;
import io.polychro.spi.Severity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Executes a single {@link Rule} against matched document nodes.
 * <p>
 * For each node matched by the rule's {@code given} expressions, applies each
 * {@code then} action: resolves the optional {@code field}, looks up the function,
 * and collects error messages as diagnostics.
 */
class RuleExecutor {

    private final JsonPathEvaluator evaluator;

    RuleExecutor(JsonPathEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    /**
     * Execute a rule against a document root, producing diagnostics for each violation.
     *
     * @param rule the rule to execute
     * @param root the document root node
     * @return diagnostics for any violations found
     */
    List<Diagnostic> execute(Rule rule, JsonNode root) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        Severity severity = mapSeverity(rule.severity());

        for (String given : rule.given()) {
            List<JsonNode> matches = evaluator.evaluate(root, given);
            for (JsonNode match : matches) {
                for (RuleAction action : rule.then()) {
                    JsonNode target = resolveField(match, action.field());
                    Optional<RuleFunction> function = BuiltinFunctions.get(action.functionName());
                    if (function.isEmpty()) {
                        continue;
                    }
                    List<String> errors = function.get().evaluate(target, action.functionOptions());
                    for (String error : errors) {
                        String message = rule.message() != null ? rule.message() : error;
                        diagnostics.add(new Diagnostic(severity, rule.name(), message, given, null));
                    }
                }
            }
        }
        return diagnostics;
    }

    JsonNode resolveField(JsonNode match, String field) {
        if (field == null || field.isEmpty()) {
            return match;
        }
        if (match == null || !match.isObject()) {
            return MissingNode.getInstance();
        }
        if (match.has(field)) {
            return match.get(field);
        }
        return MissingNode.getInstance();
    }

    static Severity mapSeverity(String severity) {
        if (severity == null) {
            return Severity.WARN;
        }
        return switch (severity.toLowerCase()) {
            case "error" -> Severity.ERROR;
            case "warn" -> Severity.WARN;
            case "info" -> Severity.INFO;
            case "hint" -> Severity.HINT;
            default -> Severity.WARN;
        };
    }
}
