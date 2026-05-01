package io.polychro.ruleset;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RuleRecordTest {

    @Test
    void ruleShouldDefaultSeverityToWarn() {
        Rule rule = new Rule("test", null, null, null, true, null, null, List.of(), List.of());
        assertEquals("warn", rule.severity());
    }

    @Test
    void ruleShouldPreserveSeverity() {
        Rule rule = new Rule("test", null, null, "error", true, null, null, List.of(), List.of());
        assertEquals("error", rule.severity());
    }

    @Test
    void ruleShouldHandleNullGiven() {
        Rule rule = new Rule("test", null, null, "warn", true, null, null, null, List.of());
        assertEquals(List.of(), rule.given());
    }

    @Test
    void ruleShouldHandleNullThen() {
        Rule rule = new Rule("test", null, null, "warn", true, null, null, List.of(), null);
        assertEquals(List.of(), rule.then());
    }

    @Test
    void ruleShouldPreserveNullFormats() {
        Rule rule = new Rule("test", null, null, "warn", true, null, null, List.of(), List.of());
        assertNull(rule.formats());
    }

    @Test
    void ruleShouldCopyFormatsWhenProvided() {
        Rule rule = new Rule("test", null, null, "warn", true, List.of("oas3"), null, List.of(), List.of());
        assertEquals(List.of("oas3"), rule.formats());
    }
}
