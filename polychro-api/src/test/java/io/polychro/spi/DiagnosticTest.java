package io.polychro.spi;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DiagnosticTest {

    @Test
    void constructionShouldStoreAllFields() {
        SourceRange range = new SourceRange(1, 1, 1, 10);
        Diagnostic d = new Diagnostic(Severity.ERROR, "rule-1", "something is wrong", "$.info", range);
        assertEquals(Severity.ERROR, d.severity());
        assertEquals("rule-1", d.code());
        assertEquals("something is wrong", d.message());
        assertEquals("$.info", d.path());
        assertEquals(range, d.range());
    }

    @Test
    void constructionShouldAllowNullPathAndRange() {
        Diagnostic d = new Diagnostic(Severity.WARN, "rule-2", "warning message", null, null);
        assertNull(d.path());
        assertNull(d.range());
    }

    @Test
    void constructionShouldAllowNullCode() {
        Diagnostic d = new Diagnostic(Severity.INFO, null, "info message", "$.paths", null);
        assertNull(d.code());
    }

    @Test
    void compareToShouldSortBySeverityDescending() {
        Diagnostic error = new Diagnostic(Severity.ERROR, null, "error", "$.a", null);
        Diagnostic warn = new Diagnostic(Severity.WARN, null, "warn", "$.a", null);
        Diagnostic info = new Diagnostic(Severity.INFO, null, "info", "$.a", null);
        Diagnostic hint = new Diagnostic(Severity.HINT, null, "hint", "$.a", null);

        assertTrue(error.compareTo(warn) < 0);
        assertTrue(warn.compareTo(info) < 0);
        assertTrue(info.compareTo(hint) < 0);
    }

    @Test
    void compareToShouldSortByPathAscendingWhenSameSeverity() {
        Diagnostic a = new Diagnostic(Severity.ERROR, null, "msg", "$.a", null);
        Diagnostic b = new Diagnostic(Severity.ERROR, null, "msg", "$.b", null);

        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
    }

    @Test
    void compareToShouldReturnZeroForEqualSeverityAndPath() {
        Diagnostic a = new Diagnostic(Severity.WARN, "r1", "msg1", "$.x", null);
        Diagnostic b = new Diagnostic(Severity.WARN, "r2", "msg2", "$.x", null);
        assertEquals(0, a.compareTo(b));
    }

    @Test
    void compareToShouldPlaceNullPathLast() {
        Diagnostic withPath = new Diagnostic(Severity.ERROR, null, "msg", "$.a", null);
        Diagnostic nullPath = new Diagnostic(Severity.ERROR, null, "msg", null, null);

        assertTrue(withPath.compareTo(nullPath) < 0);
        assertTrue(nullPath.compareTo(withPath) > 0);
    }

    @Test
    void compareToShouldReturnZeroForBothNullPaths() {
        Diagnostic a = new Diagnostic(Severity.ERROR, null, "msg1", null, null);
        Diagnostic b = new Diagnostic(Severity.ERROR, null, "msg2", null, null);
        assertEquals(0, a.compareTo(b));
    }

    @Test
    void sortingShouldProduceExpectedOrder() {
        Diagnostic hint = new Diagnostic(Severity.HINT, null, "hint", "$.z", null);
        Diagnostic errorB = new Diagnostic(Severity.ERROR, null, "error", "$.b", null);
        Diagnostic errorA = new Diagnostic(Severity.ERROR, null, "error", "$.a", null);
        Diagnostic warn = new Diagnostic(Severity.WARN, null, "warn", "$.c", null);

        List<Diagnostic> list = new ArrayList<>(List.of(hint, errorB, errorA, warn));
        Collections.sort(list);

        assertEquals(errorA, list.get(0));
        assertEquals(errorB, list.get(1));
        assertEquals(warn, list.get(2));
        assertEquals(hint, list.get(3));
    }

    @Test
    void equalityShouldWorkForIdenticalDiagnostics() {
        SourceRange range = new SourceRange(1, 1, 1, 5);
        Diagnostic a = new Diagnostic(Severity.ERROR, "r1", "msg", "$.x", range);
        Diagnostic b = new Diagnostic(Severity.ERROR, "r1", "msg", "$.x", range);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equalityShouldFailForDifferentDiagnostics() {
        Diagnostic a = new Diagnostic(Severity.ERROR, "r1", "msg", "$.x", null);
        Diagnostic b = new Diagnostic(Severity.WARN, "r1", "msg", "$.x", null);
        assertNotEquals(a, b);
    }
}
