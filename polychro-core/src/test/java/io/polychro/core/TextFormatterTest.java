package io.polychro.core;

import io.polychro.spi.Diagnostic;
import io.polychro.spi.Severity;
import io.polychro.spi.SourceRange;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextFormatterTest {

    private final TextFormatter formatter = new TextFormatter();

    @Test
    void formatShouldReturnNoIssuesForNull() {
        String result = formatter.format(null);
        assertEquals("No issues found.\n", result);
    }

    @Test
    void formatShouldReturnNoIssuesForEmptyList() {
        String result = formatter.format(List.of());
        assertEquals("No issues found.\n", result);
    }

    @Test
    void formatShouldIncludeSeverityAndMessage() {
        Diagnostic d = new Diagnostic(Severity.ERROR, null, "something broke", null, null);
        String result = formatter.format(List.of(d));
        assertTrue(result.contains("ERROR"));
        assertTrue(result.contains("something broke"));
        assertTrue(result.contains("1 issue(s) found."));
    }

    @Test
    void formatShouldIncludePathWhenPresent() {
        Diagnostic d = new Diagnostic(Severity.WARN, null, "msg", "$.root.field", null);
        String result = formatter.format(List.of(d));
        assertTrue(result.contains("at $.root.field"));
    }

    @Test
    void formatShouldIncludeCodeWhenPresent() {
        Diagnostic d = new Diagnostic(Severity.INFO, "rule-name", "msg", null, null);
        String result = formatter.format(List.of(d));
        assertTrue(result.contains("[rule-name]"));
    }

    @Test
    void formatShouldHandleAllSeverities() {
        Diagnostic error = new Diagnostic(Severity.ERROR, null, "e", null, null);
        Diagnostic warn = new Diagnostic(Severity.WARN, null, "w", null, null);
        Diagnostic info = new Diagnostic(Severity.INFO, null, "i", null, null);
        Diagnostic hint = new Diagnostic(Severity.HINT, null, "h", null, null);
        String result = formatter.format(List.of(error, warn, info, hint));
        assertTrue(result.contains("ERROR"));
        assertTrue(result.contains("WARN"));
        assertTrue(result.contains("INFO"));
        assertTrue(result.contains("HINT"));
        assertTrue(result.contains("4 issue(s) found."));
    }

    @Test
    void formatShouldShowFullLineForDiagnosticWithAllFields() {
        Diagnostic d = new Diagnostic(Severity.ERROR, "my-rule", "broken", "$.x", null);
        String result = formatter.format(List.of(d));
        assertTrue(result.contains("ERROR at $.x [my-rule]: broken"));
    }
}
