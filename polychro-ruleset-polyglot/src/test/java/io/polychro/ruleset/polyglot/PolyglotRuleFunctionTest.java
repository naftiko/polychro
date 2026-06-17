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
import org.graalvm.polyglot.Engine;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PolyglotRuleFunctionTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static Engine engine;

    @BeforeAll
    static void setUp() {
        engine = Engine.create();
    }

    @AfterAll
    static void tearDown() {
        engine.close();
    }

    @Test
    void evaluateShouldReturnEmptyForValidInput() throws Exception {
        String source = """
                export default function simpleCheck(targetVal) {
                  if (!targetVal || typeof targetVal !== "object") return [];
                  if (targetVal.name && targetVal.name.length > 0) return [];
                  return [{ message: "name is required" }];
                }
                """;
        PolyglotRuleFunction fn = new PolyglotRuleFunction("simple-check", source, "js", engine);
        JsonNode input = JSON.readTree("{\"name\": \"valid\"}");

        List<String> results = fn.evaluate(input, Map.of());
        assertTrue(results.isEmpty());
    }

    @Test
    void evaluateShouldReturnMessageForInvalidInput() throws Exception {
        String source = """
                export default function simpleCheck(targetVal) {
                  if (!targetVal || typeof targetVal !== "object") return [];
                  if (targetVal.name && targetVal.name.length > 0) return [];
                  return [{ message: "name is required" }];
                }
                """;
        PolyglotRuleFunction fn = new PolyglotRuleFunction("simple-check", source, "js", engine);
        JsonNode input = JSON.readTree("{\"name\": \"\"}");

        List<String> results = fn.evaluate(input, Map.of());
        assertEquals(1, results.size());
        assertEquals("name is required", results.get(0));
    }

    @Test
    void evaluateShouldReturnMultipleMessages() throws Exception {
        String source = """
                export default function multiResult(targetVal) {
                  return [
                    { message: "First issue" },
                    { message: "Second issue" }
                  ];
                }
                """;
        PolyglotRuleFunction fn = new PolyglotRuleFunction("multi-result", source, "js", engine);
        JsonNode input = JSON.readTree("{}");

        List<String> results = fn.evaluate(input, Map.of());
        assertEquals(2, results.size());
        assertEquals("First issue", results.get(0));
        assertEquals("Second issue", results.get(1));
    }

    @Test
    void evaluateShouldHandleNullReturn() throws Exception {
        String source = """
                export default function nullReturn(targetVal) {
                  return null;
                }
                """;
        PolyglotRuleFunction fn = new PolyglotRuleFunction("null-return", source, "js", engine);
        JsonNode input = JSON.readTree("{}");

        List<String> results = fn.evaluate(input, Map.of());
        assertTrue(results.isEmpty());
    }

    @Test
    void evaluateShouldHandleUndefinedReturn() throws Exception {
        String source = """
                export default function undefinedReturn(targetVal) {
                  if (targetVal === null) return;
                }
                """;
        PolyglotRuleFunction fn = new PolyglotRuleFunction("undefined-return", source, "js", engine);
        JsonNode input = JSON.readTree("null");

        List<String> results = fn.evaluate(input, Map.of());
        assertTrue(results.isEmpty());
    }

    @Test
    void evaluateShouldHandleEmptyArrayReturn() throws Exception {
        String source = """
                export default function emptyReturn(targetVal) {
                  return [];
                }
                """;
        PolyglotRuleFunction fn = new PolyglotRuleFunction("empty-return", source, "js", engine);
        JsonNode input = JSON.readTree("{\"info\": {}}");

        List<String> results = fn.evaluate(input, Map.of());
        assertTrue(results.isEmpty());
    }

    @Test
    void evaluateShouldHandleArrayInput() throws Exception {
        String source = """
                export default function arrayCheck(targetVal) {
                  if (Array.isArray(targetVal) && targetVal.length === 0) {
                    return [{ message: "array is empty" }];
                  }
                  return [];
                }
                """;
        PolyglotRuleFunction fn = new PolyglotRuleFunction("array-check", source, "js", engine);
        JsonNode input = JSON.readTree("[]");

        List<String> results = fn.evaluate(input, Map.of());
        assertEquals(1, results.size());
        assertEquals("array is empty", results.get(0));
    }

    @Test
    void evaluateShouldHandleNullJsonNode() throws Exception {
        String source = """
                export default function nullCheck(targetVal) {
                  if (targetVal === null || targetVal === undefined) {
                    return [{ message: "input is null" }];
                  }
                  return [];
                }
                """;
        PolyglotRuleFunction fn = new PolyglotRuleFunction("null-check", source, "js", engine);

        List<String> results = fn.evaluate(null, Map.of());
        assertEquals(1, results.size());
    }

    @Test
    void nameShouldReturnFunctionName() {
        PolyglotRuleFunction fn = new PolyglotRuleFunction("my-func", "// code", "js", engine);
        assertEquals("my-func", fn.name());
    }

    @Test
    void wrapSourceShouldStripExportDefault() {
        String source = "export default function foo(x) { return []; }";
        String wrapped = PolyglotRuleFunction.wrapSource(source, "js");
        assertFalse(wrapped.contains("export default"));
        assertTrue(wrapped.contains("function foo"));
    }

    @Test
    void wrapSourceShouldReturnAsIsForNonJs() {
        String source = "def foo(x):\n  return []";
        String wrapped = PolyglotRuleFunction.wrapSource(source, "python");
        assertEquals(source, wrapped);
    }

    @Test
    void wrapSourceShouldThrowWhenJsSourceLacksExportDefault() {
        // Validates the explicit guard added in response to review #53:
        // a JS source without `export default` must fail with a clear message
        // rather than producing a ReferenceError for __polychroFn at eval time.
        String source = "function foo(x) { return []; }";
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> PolyglotRuleFunction.wrapSource(source, "js"));
        assertTrue(ex.getMessage().contains("export default"),
                "exception message must mention the missing 'export default' keyword");
    }

    @Test
    void extractMessagesShouldHandleNullValue() {
        List<String> messages = PolyglotRuleFunction.extractMessages(null);
        assertTrue(messages.isEmpty());
    }

    @Test
    void evaluateShouldHandleNonArrayReturn() throws Exception {
        // Returns a number — not null, not isNull(), but no array elements
        String source = """
                export default function numberReturn(targetVal) {
                  return 42;
                }
                """;
        PolyglotRuleFunction fn = new PolyglotRuleFunction("number-return", source, "js", engine);
        JsonNode input = JSON.readTree("{}");

        List<String> results = fn.evaluate(input, Map.of());
        assertTrue(results.isEmpty());
    }

    @Test
    void evaluateShouldReturnEmptyWhenScriptIsNotExecutable() throws Exception {
        // A script without `export default` causes wrapSource to throw IllegalArgumentException,
        // caught by loadFunction → evaluate returns empty without propagating.
        String source = "42";
        PolyglotRuleFunction fn = new PolyglotRuleFunction("not-executable", source, "js", engine);
        JsonNode input = JSON.readTree("{}");

        List<String> results = fn.evaluate(input, Map.of());
        assertTrue(results.isEmpty());
    }

    @Test
    void evaluateShouldReturnErrorWhenScriptHasSyntaxError() throws Exception {
        String source = "export default function broken( { return []; }";
        PolyglotRuleFunction fn = new PolyglotRuleFunction("syntax-error", source, "js", engine);
        JsonNode input = JSON.readTree("{}");

        List<String> results = fn.evaluate(input, Map.of());
        // loadFunction fails and returns null → evaluate returns empty
        assertTrue(results.isEmpty());
    }

    @Test
    void extractMessagesShouldSkipItemsWithoutMessageField() throws Exception {
        // Script returns array with objects missing "message" field
        String source = """
                export default function noMessage(targetVal) {
                  return [{ path: "/a" }, { message: "valid" }, { other: true }];
                }
                """;
        PolyglotRuleFunction fn = new PolyglotRuleFunction("no-message", source, "js", engine);
        JsonNode input = JSON.readTree("{}");

        List<String> results = fn.evaluate(input, Map.of());
        assertEquals(1, results.size());
        assertEquals("valid", results.get(0));
    }

    @Test
    void wrapSourceShouldBindDefaultExportToVariableWhenLeadingJsDocPresent() {
        // Verifies that wrapSource uses variable binding (not bare return) so that
        // a leading multi-line JSDoc comment does not trigger ASI (issue #52).
        String source = """
                /**
                 * Returns violations when the value is missing.
                 *
                 * @param {*} targetVal - the node being validated
                 * @returns {Array}
                 */
                export default function foo(targetVal) {
                  return [];
                }
                """;
        String wrapped = PolyglotRuleFunction.wrapSource(source, "js");
        assertTrue(wrapped.contains("var __polychroFn ="),
                "wrapSource must use variable binding to survive a leading JSDoc comment");
        assertTrue(wrapped.contains("return __polychroFn;"),
                "wrapSource must return the bound variable at the end of the IIFE");
        assertFalse(wrapped.contains("export default"),
                "wrapSource must strip the export default keyword");
    }

    @Test
    void evaluateShouldLoadAndExecuteFunctionWithLeadingJsDoc() throws Exception {
        // Regression test for issue #52: a JSDoc block before `export default` must not
        // cause ASI to turn `return` into `return;`, silently dropping the function.
        String source = """
                /**
                 * Validates that the target value has a non-empty name field.
                 *
                 * @param {object} targetVal - the node under validation
                 * @returns {Array<{message: string}>} list of violations
                 */
                export default function namedCheck(targetVal) {
                  if (!targetVal || !targetVal.name) {
                    return [{ message: "name is required" }];
                  }
                  return [];
                }
                """;
        PolyglotRuleFunction fn = new PolyglotRuleFunction("named-check", source, "js", engine);

        JsonNode invalid = JSON.readTree("{}");
        List<String> violations = fn.evaluate(invalid, Map.of());
        assertEquals(1, violations.size(), "function with leading JSDoc must fire and return violations");
        assertEquals("name is required", violations.get(0));

        JsonNode valid = JSON.readTree("{\"name\": \"ok\"}");
        List<String> noViolations = fn.evaluate(valid, Map.of());
        assertTrue(noViolations.isEmpty(), "function with leading JSDoc must return empty list for valid input");
    }

    @Test
    void loadFunctionShouldReturnNullAndWarnWhenDefaultExportIsNotExecutable() throws Exception {
        // Covers the !canExecute() branch of loadFunction:
        // a script whose default export is a plain value (not a function) wraps to
        // `var __polychroFn = 42; return __polychroFn;` — the IIFE returns 42,
        // canExecute() is false, and the full evaluate pipeline must return empty.
        // Uses evaluate() to avoid duplicating the sandbox builder (review #53 comment).
        String source = "export default 42;";
        PolyglotRuleFunction fn = new PolyglotRuleFunction("non-fn-export", source, "js", engine);
        JsonNode input = JSON.readTree("{}");

        List<String> results = fn.evaluate(input, Map.of());
        assertTrue(results.isEmpty(),
                "evaluate must return empty when the default export is not a function");
    }
}
