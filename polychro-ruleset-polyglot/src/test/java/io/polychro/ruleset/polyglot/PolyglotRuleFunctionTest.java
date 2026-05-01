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
        // A script that evaluates to a non-callable value (number, not a function)
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
}
