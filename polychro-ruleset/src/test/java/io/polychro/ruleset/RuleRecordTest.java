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
