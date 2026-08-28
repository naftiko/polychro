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
        this(evaluator, FunctionRegistry.forRuleset(List.of()));
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
            // A key-selector expression (~) matches property NAMES, not the values they are
            // keyed to — the diagnostic's SourceRange must therefore point at the key's own
            // location, which resolveKey() resolves distinctly from resolve().
            boolean keySelector = JsonPathEvaluator.isKeySelector(given);
            for (int i = 0; i < matches.size(); i++) {
                JsonNode match = matches.get(i);
                String matchPath = pathAt(matchPaths, i, given);
                for (RuleAction action : rule.then()) {
                    JsonNode target = resolveField(match, action.field());
                    Optional<RuleFunction> function = functions.get(action.functionName());
                    if (function.isEmpty()) {
                        continue;
                    }
                    // Mirror Spectral: the diagnostic path only descends into `field` as far as
                    // that field's segments actually exist on the matched node — a single-segment
                    // field either descends fully or not at all, but a DOTTED field (e.g.
                    // "info.license.url", used by rules ported with Spectral's own
                    // `given: "$"` + nested `field:` shape) descends into each existing prefix and
                    // stops at the first missing segment, mirroring Spectral's own
                    // getClosestJsonPath trimming (verified against the real Spectral CLI: a
                    // present-but-falsy field like `url: ""` reports `info.license.url`, a
                    // missing `url` alone reports `info.license`, and a missing `license`
                    // entirely reports only `info`).
                    boolean fieldExists = fieldExists(match, action.field());
                    String effectivePath = fieldExists
                            ? effectivePath(matchPath, action.field())
                            : closestExistingFieldPath(matchPath, match, action.field());
                    List<Violation> violations =
                            function.get().evaluateViolations(target, action.functionOptions());
                    for (Violation violation : violations) {
                        // Mirror Spectral's own message fallback chain: an explicit `message:`
                        // wins; absent that, the rule's `description:` (not a generic,
                        // function-specific message) is used — verified against the real
                        // Spectral CLI: a rule with a `description` but no `message` reports the
                        // description verbatim, not "must be truthy"/"must match the pattern...".
                        // Only when BOTH are absent does Spectral fall back to a function-
                        // generated message, mirrored here by `violation.message()`. This
                        // matters beyond cosmetics: two different rules that both resolve a
                        // missing field to the same path (no `message` distinguishing them)
                        // would otherwise report the identical generic function message and be
                        // silently collapsed by Linter's DiagnosticDeduplicator.
                        //
                        // A rule's own `message:` may embed the literal `{{error}}` placeholder
                        // (Spectral's own convention — see e.g. spectral:arazzo's
                        // `message: "{{error}}"` on schema/uniqueness rules) to opt back into the
                        // per-violation function message instead of a single static string; this
                        // is required whenever a custom function can report multiple distinct
                        // violations for the same matched node (e.g. required-sections), so each
                        // violation keeps its own diagnostic instead of collapsing to one.
                        String message = rule.message() != null ? interpolateError(rule.message(), violation.message())
                                : rule.description() != null ? rule.description()
                                : violation.message();
                        String violationPath = combinePath(effectivePath, violation.path());
                        SourceRange range = keySelector
                                ? sourceMap.resolveKey(violationPath)
                                : sourceMap.resolve(violationPath);
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
     * <p>{@code field} may itself be a <strong>dotted</strong> field path (e.g.
     * {@code "info.license.url"}, mirroring Spectral's own lodash-style field lookup in
     * {@code getLintTargets}) — every segment is appended verbatim, separated by {@code '.'}.
     * Each segment must be a <em>simple identifier</em> — a plain object key — never a JSONPath
     * expression or an array-index notation; a bracket expression such as {@code [0]} would
     * yield an invalid path like {@code $.info.[0]}. The only caller passes a field resolved
     * from the rule action, which satisfies this contract.
     */
    static String effectivePath(String matchPath, String field) {
        if (field == null || field.isEmpty()) {
            return matchPath;
        }
        return matchPath + "." + field;
    }

    /**
     * Composes the diagnostic path for a dotted {@code field} whose segments only partially
     * exist on {@code match} — mirrors Spectral's {@code getClosestJsonPath} trimming: descend
     * into every existing prefix segment and stop at (excluding) the first missing one, so a
     * rule like {@code info-license} ({@code given: "$"}, {@code field: "info.license"})
     * reports {@code $.info} when {@code info} exists but has no {@code license} key, rather
     * than the matched node's own path unconditionally.
     *
     * @param matchPath the matched node's own path (e.g. {@code "$"})
     * @param match     the matched node itself
     * @param field     the rule action's field, possibly dotted; {@code null}/empty is handled
     *                  by the caller via {@link #fieldExists}, which is always {@code false} for it
     * @return {@code matchPath} plus every existing leading segment of {@code field}
     */
    static String closestExistingFieldPath(String matchPath, JsonNode match, String field) {
        if (field == null || field.isEmpty()) {
            return matchPath;
        }
        StringBuilder path = new StringBuilder(matchPath);
        JsonNode current = match;
        for (String segment : field.split("\\.")) {
            if (current == null || !current.isObject() || !current.has(segment)) {
                break;
            }
            current = current.get(segment);
            path.append('.').append(segment);
        }
        return path.toString();
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
        JsonNode current = match;
        for (String segment : field.split("\\.")) {
            if (current == null || !current.isObject() || !current.has(segment)) {
                return MissingNode.getInstance();
            }
            current = current.get(segment);
        }
        return current;
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

    /**
     * @return {@code true} when {@code field} is a non-empty field path and every one of its
     *         (possibly dotted) segments resolves on {@code match} — the same condition that
     *         determines whether Spectral's diagnostic path descends fully into the field (see
     *         the caller in {@link #execute(Rule, Document)}). {@code false} for a
     *         {@code null}/empty field (nothing to descend into), a non-object match, or a
     *         field whose chain is missing at any segment — a present-but-falsy leaf value
     *         (e.g. {@code ""}, {@code null} JSON literal) still counts as existing, since only
     *         the key's presence is checked, not its value.
     */
    static boolean fieldExists(JsonNode match, String field) {
        if (field == null || field.isEmpty()) {
            return false;
        }
        JsonNode current = match;
        for (String segment : field.split("\\.")) {
            if (current == null || !current.isObject() || !current.has(segment)) {
                return false;
            }
            current = current.get(segment);
        }
        return true;
    }

    /**
     * Substitutes the literal {@code {{error}}} placeholder in a rule's {@code message:} template
     * with the current violation's function-generated message, mirroring Spectral's own
     * {@code message: "{{error}}"} convention (e.g. {@code spectral:arazzo}'s schema/uniqueness
     * rules) for surfacing a dynamic, per-violation message while still allowing the rest of the
     * template to be static text. A {@code message:} with no {@code {{error}}} occurrence is
     * returned unchanged — the common case of a single, static message string.
     *
     * @param messageTemplate the rule's own {@code message:} (never {@code null}; caller guards)
     * @param errorMessage    the current violation's function-generated message
     */
    static String interpolateError(String messageTemplate, String errorMessage) {
        if (!messageTemplate.contains("{{error}}")) {
            return messageTemplate;
        }
        return messageTemplate.replace("{{error}}", errorMessage != null ? errorMessage : "");
    }
}
