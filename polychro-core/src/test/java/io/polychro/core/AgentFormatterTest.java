package io.polychro.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.polychro.spi.Diagnostic;
import io.polychro.spi.Severity;
import io.polychro.spi.SourceRange;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AgentFormatterTest {

    private final AgentFormatter formatter = new AgentFormatter();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void formatShouldReturnEmptyDiagnosticsForNull() throws Exception {
        String result = formatter.format(null);
        JsonNode root = mapper.readTree(result);
        assertTrue(root.has("diagnostics"));
        assertEquals(0, root.get("diagnostics").size());
        assertEquals(0, root.get("summary").get("errors").asInt());
        assertEquals(0, root.get("summary").get("warnings").asInt());
        assertEquals(0, root.get("summary").get("info").asInt());
        assertTrue(root.has("tokens"));
        assertTrue(root.get("tokens").asInt() > 0);
    }

    @Test
    void formatShouldReturnEmptyDiagnosticsForEmptyList() throws Exception {
        String result = formatter.format(List.of());
        JsonNode root = mapper.readTree(result);
        assertEquals(0, root.get("diagnostics").size());
        assertEquals(0, root.get("summary").get("errors").asInt());
    }

    @Test
    void formatShouldIncludeSeverityAndMessage() throws Exception {
        Diagnostic d = new Diagnostic(Severity.ERROR, "no-trailing-slash",
                "Path must not end with '/'", "$.consumes[0].path", null);

        String result = formatter.format(List.of(d));
        JsonNode root = mapper.readTree(result);
        JsonNode diag = root.get("diagnostics").get(0);

        assertEquals("error", diag.get("severity").asText());
        assertEquals("no-trailing-slash", diag.get("rule").asText());
        assertEquals("Path must not end with '/'", diag.get("message").asText());
        assertEquals("$.consumes[0].path", diag.get("path").asText());
    }

    @Test
    void formatShouldIncludeRangeWhenPresent() throws Exception {
        SourceRange range = new SourceRange(42, 12, 42, 24);
        Diagnostic d = new Diagnostic(Severity.WARN, "rule", "msg", null, range);

        String result = formatter.format(List.of(d));
        JsonNode root = mapper.readTree(result);
        JsonNode rangeNode = root.get("diagnostics").get(0).get("range");

        assertNotNull(rangeNode);
        assertEquals(42, rangeNode.get("startLine").asInt());
        assertEquals(12, rangeNode.get("startColumn").asInt());
        assertEquals(42, rangeNode.get("endLine").asInt());
        assertEquals(24, rangeNode.get("endColumn").asInt());
    }

    @Test
    void formatShouldOmitRangeWhenNull() throws Exception {
        Diagnostic d = new Diagnostic(Severity.INFO, "rule", "msg", null, null);

        String result = formatter.format(List.of(d));
        JsonNode root = mapper.readTree(result);
        assertFalse(root.get("diagnostics").get(0).has("range"));
    }

    @Test
    void formatShouldOmitPathWhenNull() throws Exception {
        Diagnostic d = new Diagnostic(Severity.ERROR, "rule", "msg", null, null);

        String result = formatter.format(List.of(d));
        JsonNode root = mapper.readTree(result);
        assertFalse(root.get("diagnostics").get(0).has("path"));
    }

    @Test
    void formatShouldOmitRuleWhenCodeNull() throws Exception {
        Diagnostic d = new Diagnostic(Severity.ERROR, null, "msg", null, null);

        String result = formatter.format(List.of(d));
        JsonNode root = mapper.readTree(result);
        assertFalse(root.get("diagnostics").get(0).has("rule"));
    }

    @Test
    void formatShouldCountMultipleDiagnostics() throws Exception {
        List<Diagnostic> diagnostics = List.of(
                new Diagnostic(Severity.ERROR, "r1", "e1", null, null),
                new Diagnostic(Severity.WARN, "r2", "w1", null, null),
                new Diagnostic(Severity.WARN, "r3", "w2", null, null),
                new Diagnostic(Severity.INFO, "r4", "i1", null, null),
                new Diagnostic(Severity.HINT, "r5", "h1", null, null)
        );

        String result = formatter.format(diagnostics);
        JsonNode root = mapper.readTree(result);

        assertEquals(5, root.get("diagnostics").size());
        assertEquals(1, root.get("summary").get("errors").asInt());
        assertEquals(2, root.get("summary").get("warnings").asInt());
        assertEquals(2, root.get("summary").get("info").asInt()); // INFO + HINT both count as info
    }

    @Test
    void formatShouldIncludeSuggestionForMustNotMessage() throws Exception {
        Diagnostic d = new Diagnostic(Severity.ERROR, "rule",
                "Path must not end with '/'", null, null);

        String result = formatter.format(List.of(d));
        JsonNode root = mapper.readTree(result);
        JsonNode diag = root.get("diagnostics").get(0);

        assertTrue(diag.has("suggestion"));
        assertEquals("Fix: Path must not end with '/'", diag.get("suggestion").asText());
    }

    @Test
    void formatShouldIncludeSuggestionForMissingMessage() throws Exception {
        Diagnostic d = new Diagnostic(Severity.WARN, "rule",
                "Missing required field: description", null, null);

        String result = formatter.format(List.of(d));
        JsonNode root = mapper.readTree(result);
        JsonNode diag = root.get("diagnostics").get(0);

        assertTrue(diag.has("suggestion"));
        assertEquals("Add the description", diag.get("suggestion").asText());
    }

    @Test
    void formatShouldIncludeSuggestionForInvalidMessage() throws Exception {
        Diagnostic d = new Diagnostic(Severity.ERROR, "rule",
                "Invalid value: expected string", null, null);

        String result = formatter.format(List.of(d));
        JsonNode root = mapper.readTree(result);
        JsonNode diag = root.get("diagnostics").get(0);

        assertTrue(diag.has("suggestion"));
        assertEquals("Correct the expected string", diag.get("suggestion").asText());
    }

    @Test
    void formatShouldOmitSuggestionForGenericMessage() throws Exception {
        Diagnostic d = new Diagnostic(Severity.ERROR, "rule",
                "Something else entirely", null, null);

        String result = formatter.format(List.of(d));
        JsonNode root = mapper.readTree(result);
        assertFalse(root.get("diagnostics").get(0).has("suggestion"));
    }

    @Test
    void formatShouldOmitSuggestionWhenCodeNull() throws Exception {
        Diagnostic d = new Diagnostic(Severity.ERROR, null,
                "Path must not end with '/'", null, null);

        String result = formatter.format(List.of(d));
        JsonNode root = mapper.readTree(result);
        assertFalse(root.get("diagnostics").get(0).has("suggestion"));
    }

    @Test
    void formatShouldOmitSuggestionWhenMessageNull() throws Exception {
        Diagnostic d = new Diagnostic(Severity.ERROR, "rule", null, null, null);

        String result = formatter.format(List.of(d));
        JsonNode root = mapper.readTree(result);
        assertFalse(root.get("diagnostics").get(0).has("suggestion"));
    }

    @Test
    void formatShouldIncludeSuggestionForShouldNotMessage() throws Exception {
        Diagnostic d = new Diagnostic(Severity.WARN, "rule",
                "Name should not be empty", null, null);

        String result = formatter.format(List.of(d));
        JsonNode root = mapper.readTree(result);
        JsonNode diag = root.get("diagnostics").get(0);

        assertTrue(diag.has("suggestion"));
        assertEquals("Fix: Name should not be empty", diag.get("suggestion").asText());
    }

    @Test
    void formatShouldIncludeSuggestionForLowercaseMissing() throws Exception {
        Diagnostic d = new Diagnostic(Severity.WARN, "rule",
                "A missing field: name", null, null);

        String result = formatter.format(List.of(d));
        JsonNode root = mapper.readTree(result);
        JsonNode diag = root.get("diagnostics").get(0);

        assertTrue(diag.has("suggestion"));
        assertEquals("Add the name", diag.get("suggestion").asText());
    }

    @Test
    void formatShouldIncludeSuggestionForLowercaseInvalid() throws Exception {
        Diagnostic d = new Diagnostic(Severity.ERROR, "rule",
                "Found invalid value: type mismatch", null, null);

        String result = formatter.format(List.of(d));
        JsonNode root = mapper.readTree(result);
        JsonNode diag = root.get("diagnostics").get(0);

        assertTrue(diag.has("suggestion"));
        assertEquals("Correct the type mismatch", diag.get("suggestion").asText());
    }

    @Test
    void formatShouldHandleMissingMessageWithoutColon() throws Exception {
        Diagnostic d = new Diagnostic(Severity.WARN, "rule",
                "A missing element", null, null);

        String result = formatter.format(List.of(d));
        JsonNode root = mapper.readTree(result);
        JsonNode diag = root.get("diagnostics").get(0);

        assertTrue(diag.has("suggestion"));
        assertEquals("Add the a missing element", diag.get("suggestion").asText());
    }

    @Test
    void formatShouldHandleMessageWithColonAtEnd() throws Exception {
        // Colon at the very end — colonIndex == message.length()-1
        Diagnostic d = new Diagnostic(Severity.WARN, "rule",
                "missing value:", null, null);

        String result = formatter.format(List.of(d));
        JsonNode root = mapper.readTree(result);
        JsonNode diag = root.get("diagnostics").get(0);

        assertTrue(diag.has("suggestion"));
        // colonIndex = 13, message.length()-1 = 13 → condition false → lowercase
        assertEquals("Add the missing value:", diag.get("suggestion").asText());
    }

    @Test
    void formatShouldProduceValidJson() throws Exception {
        Diagnostic d = new Diagnostic(Severity.ERROR, "rule", "msg", "$.path", null);

        String result = formatter.format(List.of(d));
        // Must be parseable JSON
        assertDoesNotThrow(() -> mapper.readTree(result));
        // Must not contain ANSI codes
        assertFalse(result.contains("\u001B["));
    }

    @Test
    void mapSeverityShouldMapAllValues() {
        assertEquals("error", AgentFormatter.mapSeverity(Severity.ERROR));
        assertEquals("warning", AgentFormatter.mapSeverity(Severity.WARN));
        assertEquals("info", AgentFormatter.mapSeverity(Severity.INFO));
        assertEquals("hint", AgentFormatter.mapSeverity(Severity.HINT));
    }
}
