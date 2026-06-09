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
package io.polychro.ruleset.polyglot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.polychro.ruleset.RuleFunction;
import io.polychro.ruleset.Violation;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.ResourceLimits;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.IOAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A {@link RuleFunction} backed by a GraalVM polyglot script.
 * <p>
 * The script is executed in a sandboxed context with no host access,
 * no I/O, and configurable resource limits (statement count).
 */
class PolyglotRuleFunction implements RuleFunction {

    private static final Logger LOG = LoggerFactory.getLogger(PolyglotRuleFunction.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final long STATEMENT_LIMIT = 1_000_000L;

    private final String functionName;
    private final String sourceCode;
    private final String languageId;
    private final Engine engine;

    PolyglotRuleFunction(String functionName, String sourceCode, String languageId, Engine engine) {
        this.functionName = functionName;
        this.sourceCode = sourceCode;
        this.languageId = languageId;
        this.engine = engine;
    }

    @Override
    public String name() {
        return functionName;
    }

    @Override
    public List<String> evaluate(JsonNode targetNode, Map<String, Object> options) {
        return evaluateViolations(targetNode, options).stream()
                .map(Violation::message)
                .toList();
    }

    @Override
    public List<Violation> evaluateViolations(JsonNode targetNode, Map<String, Object> options) {
        Context context = createSandboxedContext();
        try {
            Value function = loadFunction(context);
            if (function == null) {
                return List.of();
            }

            String jsonInput = MAPPER.writeValueAsString(targetNode);
            Value jsInput = context.eval("js", "(" + jsonInput + ")");

            Value result = function.execute(jsInput);
            return extractViolations(result);
        } catch (Exception e) {
            LOG.warn("Error executing polyglot function '{}': {}", functionName, e.getMessage());
            return List.of(Violation.of(
                    "Function '" + functionName + "' execution failed: " + e.getMessage()));
        } finally {
            try {
                context.close();
            } catch (Exception closeEx) {
                LOG.debug("Error closing polyglot context for '{}': {}", functionName, closeEx.getMessage());
            }
        }
    }

    private Context createSandboxedContext() {
        ResourceLimits limits = ResourceLimits.newBuilder()
                .statementLimit(STATEMENT_LIMIT, null)
                .build();

        return Context.newBuilder(languageId)
                .engine(engine)
                .allowHostAccess(HostAccess.NONE)
                .allowIO(IOAccess.NONE)
                .allowCreateThread(false)
                .allowNativeAccess(false)
                .allowHostClassLookup(className -> false)
                .resourceLimits(limits)
                .build();
    }

    Value loadFunction(Context context) {
        // Wrap ES module export default in a way GraalVM can execute
        String wrappedSource = wrapSource(sourceCode, languageId);
        try {
            Source source = Source.newBuilder(languageId, wrappedSource, functionName + ".wrapped")
                    .buildLiteral();
            Value result = context.eval(source);
            if (result.canExecute()) {
                return result;
            }
            LOG.warn("Script for function '{}' did not produce an executable value", functionName);
            return null;
        } catch (Exception e) {
            LOG.warn("Failed to load function '{}': {}", functionName, e.getMessage());
            return null;
        }
    }

    static String wrapSource(String source, String languageId) {
        if ("js".equals(languageId)) {
            // Strip "export default" and wrap in IIFE that returns the function
            String stripped = source.replaceFirst("(?s)export\\s+default\\s+", "");
            return "(function() { return " + stripped + " })()";
        }
        // Python and Groovy: source is executed directly, function name extracted differently
        return source;
    }

    /**
     * Convert a script's result array into the {@code message} strings only, dropping any per-result
     * {@code path}. Retained as a thin shim over {@link #extractViolations(Value)} for callers (and
     * tests) that only care about messages.
     */
    static List<String> extractMessages(Value result) {
        return extractViolations(result).stream().map(Violation::message).toList();
    }

    /**
     * Convert a script's result array into {@link Violation}s, capturing both the {@code message}
     * and the optional {@code path} each result object carries (issue #32, Layer 1).
     *
     * <p>The {@code path} may be a string already in dot/bracket notation, or an array of segments
     * (e.g. {@code ["consumes", 0, "namespace"]}) as the Naftiko functions emit; in the latter case
     * it is normalised to a relative path ({@code consumes[0].namespace}). Items without a
     * {@code message} are skipped.
     */
    static List<Violation> extractViolations(Value result) {
        List<Violation> violations = new ArrayList<>();
        if (result == null || result.isNull() || !result.hasArrayElements()) {
            return violations;
        }
        long size = result.getArraySize();
        for (long i = 0; i < size; i++) {
            Value item = result.getArrayElement(i);
            if (item.hasMember("message")) {
                String message = item.getMember("message").asString();
                String path = extractPath(item.hasMember("path") ? item.getMember("path") : null);
                violations.add(Violation.at(message, path));
            }
        }
        return violations;
    }

    /**
     * Normalise a result object's {@code path} member to a relative dot/bracket path, or
     * {@code null} when absent. Array paths are joined segment by segment: string segments are
     * appended after a {@code '.'}, numeric segments as {@code [index]}. Segments that are neither
     * a number nor a string (a malformed path the Naftiko functions never emit) are skipped.
     */
    static String extractPath(Value path) {
        if (path == null || path.isNull()) {
            return null;
        }
        if (path.isString()) {
            String s = path.asString();
            return s.isEmpty() ? null : s;
        }
        if (!path.hasArrayElements()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        long size = path.getArraySize();
        for (long i = 0; i < size; i++) {
            Value segment = path.getArrayElement(i);
            if (segment.isNumber()) {
                sb.append('[').append(segment.asLong()).append(']');
            } else if (segment.isString()) {
                if (sb.length() > 0) {
                    sb.append('.');
                }
                sb.append(segment.asString());
            }
            // Other segment types are skipped rather than throwing on asString().
        }
        return sb.length() == 0 ? null : sb.toString();
    }
}
