package io.polychro.ruleset;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RuleActionRecordTest {

    @Test
    void ruleActionShouldHandleNullFunctionOptions() {
        RuleAction action = new RuleAction("field", "truthy", null);
        assertEquals(Map.of(), action.functionOptions());
    }

    @Test
    void ruleActionShouldPreserveNonNullValues() {
        RuleAction action = new RuleAction("name", "pattern", Map.of("match", "^test"));
        assertEquals("name", action.field());
        assertEquals("pattern", action.functionName());
        assertEquals(Map.of("match", "^test"), action.functionOptions());
    }
}
