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
import io.polychro.spi.Document;
import io.polychro.spi.SourceMap;
import io.polychro.spi.SourceRange;
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
    private final FunctionRegistry functions;

    RuleExecutor(JsonPathEvaluator evaluator) {
        this(evaluator, FunctionRegistry.forRuleset(null, List.of()));
    }

    RuleExecutor(JsonPathEvaluator evaluator, FunctionRegistry functions) {
        this.evaluator = evaluator;
        this.functions = functions;
    }

    /**
     * Execute a rule against a bare document root, with no source map. Diagnostics produced this
     * way carry a {@code null} range (the same graceful fallback as an unresolvable path). Provided
     * for unit tests and callers that hold only a parsed {@link JsonNode} without the originating
     * {@link Document}; production validation goes through {@link #execute(Rule, Document)}.
     *
     * <p>The synthetic {@link Document} carries {@link SourceMap#NONE} precisely so range resolution
     * is skipped: every lookup returns a {@code null} range and the {@code null} format is never
     * read. This path therefore cannot throw.
     *
     * @param rule the rule to execute
     * @param root the document root node
     * @return diagnostics for any violations found
     */
    List<Diagnostic> execute(Rule rule, JsonNode root) {
        return execute(rule, new Document(root, null, null, SourceMap.NONE, null));
    }

    /**
     * Execute a rule against a document, producing diagnostics for each violation.
     *
     * <p>For every match the concrete path (e.g. {@code $.consumes[0].baseUri}) is resolved against
     * the document's {@link SourceMap} so the resulting {@link Diagnostic} carries a
     * {@link SourceRange} (issue #32). If the source map cannot locate a path, the range falls back
     * to {@code null} — no regression for unresolvable paths.
     *
     * @param rule the rule to execute
     * @param doc  the document under validation
     * @return diagnostics for any violations found
     */
    List<Diagnostic> execute(Rule rule, Document doc) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        Severity severity = mapSeverity(rule.severity());
        JsonNode root = doc.root();
        SourceMap sourceMap = doc.sourceMap();

        for (String given : rule.given()) {
            List<JsonNode> matches = evaluator.evaluate(root, given);
            List<String> matchPaths = evaluator.evaluatePaths(root, given);
            for (int i = 0; i < matches.size(); i++) {
                JsonNode match = matches.get(i);
                String matchPath = pathAt(matchPaths, i, given);
                for (RuleAction action : rule.then()) {
                    JsonNode target = resolveField(match, action.field());
                    Optional<RuleFunction> function = functions.get(action.functionName());
                    if (function.isEmpty()) {
                        continue;
                    }
                    String effectivePath = effectivePath(matchPath, action.field());
                    List<Violation> violations =
                            function.get().evaluateViolations(target, action.functionOptions());
                    for (Violation violation : violations) {
                        String message = rule.message() != null ? rule.message() : violation.message();
                        String violationPath = combinePath(effectivePath, violation.path());
                        SourceRange range = sourceMap.resolve(violationPath);
                        diagnostics.add(new Diagnostic(severity, rule.name(), message,
                                violationPath, range));
                    }
                }
            }
        }
        return diagnostics;
    }

    /**
     * Compose the path of the actual evaluated node: the matched path, plus the resolved
     * {@code field} segment when the action narrows into a child field.
     *
     * <p>{@code field} must be a <em>simple identifier</em> — a plain object key — never a JSONPath
     * expression or an array-index notation. It is appended verbatim after a {@code '.'}, so a
     * bracket expression such as {@code [0]} would yield an invalid path like {@code $.info.[0]}.
     * The only caller passes a child field name resolved from the rule action, which satisfies this
     * contract.
     */
    static String effectivePath(String matchPath, String field) {
        if (field == null || field.isEmpty()) {
            return matchPath;
        }
        return matchPath + "." + field;
    }

    /**
     * Select the concrete path for the match at index {@code i}. {@link JsonPathEvaluator#evaluate}
     * and {@link JsonPathEvaluator#evaluatePaths} return lists of the same cardinality for the same
     * expression, so {@code matchPaths.get(i)} is normally present; the fallback to the {@code given}
     * selector guards the rare case where the path list is shorter, keeping a non-null path.
     */
    static String pathAt(List<String> matchPaths, int i, String given) {
        return i < matchPaths.size() ? matchPaths.get(i) : given;
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

    /**
     * Append a violation's optional <em>relative</em> path to the matched node's path so the
     * source map can resolve the precise offender (issue #32, Layer 1).
     *
     * <p>The relative path uses dot/bracket notation rooted at the matched node
     * (e.g. {@code consumes[0].namespace}). A {@code null} or empty relative path means the
     * violation refers to the matched node itself, so {@code basePath} is returned unchanged —
     * the pre-existing behaviour for built-in functions. A relative segment that already starts
     * with {@code [} (an array index) is appended without a separating dot to avoid an invalid
     * path like {@code $.consumes.[0]}.
     */
    static String combinePath(String basePath, String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            return basePath;
        }
        if (relativePath.startsWith("[")) {
            return basePath + relativePath;
        }
        return basePath + "." + relativePath;
    }
}
