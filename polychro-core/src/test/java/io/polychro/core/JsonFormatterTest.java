package io.polychro.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.polychro.spi.Diagnostic;
import io.polychro.spi.Severity;
import io.polychro.spi.SourceRange;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonFormatterTest {

    private final JsonFormatter formatter = new JsonFormatter();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void formatShouldReturnEmptyArrayForNull() throws Exception {
        String result = formatter.format(null);
        JsonNode node = mapper.readTree(result);
        assertTrue(node.isArray());
        assertEquals(0, node.size());
    }

    @Test
    void formatShouldReturnEmptyArrayForEmptyList() throws Exception {
        String result = formatter.format(List.of());
        JsonNode node = mapper.readTree(result);
        assertTrue(node.isArray());
        assertEquals(0, node.size());
    }

    @Test
    void formatShouldIncludeSeverityAndMessage() throws Exception {
        Diagnostic d = new Diagnostic(Severity.ERROR, null, "test msg", null, null);
        String result = formatter.format(List.of(d));
        JsonNode node = mapper.readTree(result);
        assertEquals(1, node.size());
        assertEquals("error", node.get(0).get("severity").asText());
        assertEquals("test msg", node.get(0).get("message").asText());
    }

    @Test
    void formatShouldIncludeCodeWhenPresent() throws Exception {
        Diagnostic d = new Diagnostic(Severity.WARN, "rule-x", "msg", null, null);
        String result = formatter.format(List.of(d));
        JsonNode node = mapper.readTree(result);
        assertEquals("rule-x", node.get(0).get("code").asText());
    }

    @Test
    void formatShouldOmitCodeWhenNull() throws Exception {
        Diagnostic d = new Diagnostic(Severity.WARN, null, "msg", null, null);
        String result = formatter.format(List.of(d));
        JsonNode node = mapper.readTree(result);
        assertFalse(node.get(0).has("code"));
    }

    @Test
    void formatShouldIncludePathWhenPresent() throws Exception {
        Diagnostic d = new Diagnostic(Severity.INFO, null, "msg", "$.root", null);
        String result = formatter.format(List.of(d));
        JsonNode node = mapper.readTree(result);
        assertEquals("$.root", node.get(0).get("path").asText());
    }

    @Test
    void formatShouldOmitPathWhenNull() throws Exception {
        Diagnostic d = new Diagnostic(Severity.INFO, null, "msg", null, null);
        String result = formatter.format(List.of(d));
        JsonNode node = mapper.readTree(result);
        assertFalse(node.get(0).has("path"));
    }

    @Test
    void formatShouldIncludeRangeWhenPresent() throws Exception {
        SourceRange range = new SourceRange(1, 2, 3, 4);
        Diagnostic d = new Diagnostic(Severity.ERROR, null, "msg", null, range);
        String result = formatter.format(List.of(d));
        JsonNode node = mapper.readTree(result);
        JsonNode rangeNode = node.get(0).get("range");
        assertNotNull(rangeNode);
        assertEquals(1, rangeNode.get("startLine").asInt());
        assertEquals(2, rangeNode.get("startColumn").asInt());
        assertEquals(3, rangeNode.get("endLine").asInt());
        assertEquals(4, rangeNode.get("endColumn").asInt());
    }

    @Test
    void formatShouldOmitRangeWhenNull() throws Exception {
        Diagnostic d = new Diagnostic(Severity.ERROR, null, "msg", null, null);
        String result = formatter.format(List.of(d));
        JsonNode node = mapper.readTree(result);
        assertFalse(node.get(0).has("range"));
    }

    @Test
    void formatShouldHandleMultipleDiagnostics() throws Exception {
        Diagnostic d1 = new Diagnostic(Severity.ERROR, "c1", "msg1", "$.a", null);
        Diagnostic d2 = new Diagnostic(Severity.WARN, "c2", "msg2", "$.b", null);
        String result = formatter.format(List.of(d1, d2));
        JsonNode node = mapper.readTree(result);
        assertEquals(2, node.size());
    }
}
