package io.polychro.core;

import io.polychro.spi.Diagnostic;
import io.polychro.spi.Severity;
import io.polychro.spi.SourceRange;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DiagnosticDeduplicatorTest {

    @Test
    void deduplicateShouldReturnEmptyForNullInput() {
        List<Diagnostic> result = DiagnosticDeduplicator.deduplicate(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void deduplicateShouldReturnSameListForSingleElement() {
        Diagnostic d = new Diagnostic(Severity.ERROR, "code1", "msg", "$.path", null);
        List<Diagnostic> input = List.of(d);
        List<Diagnostic> result = DiagnosticDeduplicator.deduplicate(input);
        assertEquals(1, result.size());
        assertSame(d, result.get(0));
    }

    @Test
    void deduplicateShouldReturnEmptyForEmptyInput() {
        List<Diagnostic> result = DiagnosticDeduplicator.deduplicate(List.of());
        assertTrue(result.isEmpty());
    }

    @Test
    void deduplicateShouldRemoveDuplicatesSamePathAndMessage() {
        Diagnostic d1 = new Diagnostic(Severity.WARN, "code1", "duplicate msg", "$.field", null);
        Diagnostic d2 = new Diagnostic(Severity.WARN, "code2", "duplicate msg", "$.field", null);
        List<Diagnostic> input = List.of(d1, d2);
        List<Diagnostic> result = DiagnosticDeduplicator.deduplicate(input);
        assertEquals(1, result.size());
        assertEquals("duplicate msg", result.get(0).message());
    }

    @Test
    void deduplicateShouldKeepHighestSeverityOnDuplicate() {
        Diagnostic warn = new Diagnostic(Severity.WARN, "c1", "msg", "$.x", null);
        Diagnostic error = new Diagnostic(Severity.ERROR, "c2", "msg", "$.x", null);
        List<Diagnostic> input = new ArrayList<>();
        input.add(warn);
        input.add(error);
        List<Diagnostic> result = DiagnosticDeduplicator.deduplicate(input);
        assertEquals(1, result.size());
        assertEquals(Severity.ERROR, result.get(0).severity());
    }

    @Test
    void deduplicateShouldKeepDistinctMessages() {
        Diagnostic d1 = new Diagnostic(Severity.WARN, "c1", "msg1", "$.a", null);
        Diagnostic d2 = new Diagnostic(Severity.WARN, "c1", "msg2", "$.a", null);
        List<Diagnostic> input = List.of(d1, d2);
        List<Diagnostic> result = DiagnosticDeduplicator.deduplicate(input);
        assertEquals(2, result.size());
    }

    @Test
    void deduplicateShouldKeepDistinctPaths() {
        Diagnostic d1 = new Diagnostic(Severity.ERROR, "c1", "msg", "$.a", null);
        Diagnostic d2 = new Diagnostic(Severity.ERROR, "c1", "msg", "$.b", null);
        List<Diagnostic> input = List.of(d1, d2);
        List<Diagnostic> result = DiagnosticDeduplicator.deduplicate(input);
        assertEquals(2, result.size());
    }

    @Test
    void deduplicateShouldHandleNullPathAndMessage() {
        Diagnostic d1 = new Diagnostic(Severity.INFO, null, null, null, null);
        Diagnostic d2 = new Diagnostic(Severity.ERROR, null, null, null, null);
        List<Diagnostic> input = new ArrayList<>();
        input.add(d1);
        input.add(d2);
        List<Diagnostic> result = DiagnosticDeduplicator.deduplicate(input);
        assertEquals(1, result.size());
        assertEquals(Severity.ERROR, result.get(0).severity());
    }

    @Test
    void deduplicateShouldPreserveOrderForNonDuplicates() {
        Diagnostic d1 = new Diagnostic(Severity.ERROR, "c1", "msg1", "$.a", null);
        Diagnostic d2 = new Diagnostic(Severity.WARN, "c2", "msg2", "$.b", null);
        Diagnostic d3 = new Diagnostic(Severity.INFO, "c3", "msg3", "$.c", null);
        List<Diagnostic> input = List.of(d1, d2, d3);
        List<Diagnostic> result = DiagnosticDeduplicator.deduplicate(input);
        assertEquals(3, result.size());
        assertEquals("msg1", result.get(0).message());
        assertEquals("msg2", result.get(1).message());
        assertEquals("msg3", result.get(2).message());
    }

    @Test
    void deduplicateShouldNotUpgradeWhenExistingHasHigherSeverity() {
        Diagnostic error = new Diagnostic(Severity.ERROR, "c1", "msg", "$.x", null);
        Diagnostic warn = new Diagnostic(Severity.WARN, "c2", "msg", "$.x", null);
        List<Diagnostic> input = new ArrayList<>();
        input.add(error);
        input.add(warn);
        List<Diagnostic> result = DiagnosticDeduplicator.deduplicate(input);
        assertEquals(1, result.size());
        assertEquals(Severity.ERROR, result.get(0).severity());
    }
}
