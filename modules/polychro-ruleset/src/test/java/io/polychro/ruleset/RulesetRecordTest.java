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

class RulesetRecordTest {

    @Test
    void rulesetShouldHandleNullExtendsRefs() {
        Ruleset ruleset = new Ruleset(null, Map.of(), List.of(), List.of(), null, List.of(), Map.of(), null);
        assertEquals(List.of(), ruleset.extendsRefs());
    }

    @Test
    void rulesetShouldHandleNullAliases() {
        Ruleset ruleset = new Ruleset(List.of(), null, List.of(), List.of(), null, List.of(), Map.of(), null);
        assertEquals(Map.of(), ruleset.aliases());
    }

    @Test
    void rulesetShouldHandleNullOverrides() {
        Ruleset ruleset = new Ruleset(List.of(), Map.of(), null, List.of(), null, List.of(), Map.of(), null);
        assertEquals(List.of(), ruleset.overrides());
    }

    @Test
    void rulesetShouldHandleNullFormats() {
        Ruleset ruleset = new Ruleset(List.of(), Map.of(), List.of(), null, null, List.of(), Map.of(), null);
        assertEquals(List.of(), ruleset.formats());
    }

    @Test
    void rulesetShouldHandleNullFunctions() {
        Ruleset ruleset = new Ruleset(List.of(), Map.of(), List.of(), List.of(), null, null, Map.of(), null);
        assertEquals(List.of(), ruleset.functions());
    }

    @Test
    void rulesetShouldHandleNullRules() {
        Ruleset ruleset = new Ruleset(List.of(), Map.of(), List.of(), List.of(), null, List.of(), null, null);
        assertEquals(Map.of(), ruleset.rules());
    }

    @Test
    void rulesetShouldPreserveNonNullValues() {
        Ruleset ruleset = new Ruleset(
                List.of("a"), Map.of("k", "v"), List.of(), List.of("f"), "./dir",
                List.of("fn"), Map.of("r", new Rule("r", null, null, "warn", true, null, null, List.of(), List.of())),
                "http://docs"
        );
        assertEquals(List.of("a"), ruleset.extendsRefs());
        assertEquals(Map.of("k", "v"), ruleset.aliases());
        assertEquals(List.of(), ruleset.overrides());
        assertEquals(List.of("f"), ruleset.formats());
        assertEquals("./dir", ruleset.functionsDir());
        assertEquals(List.of("fn"), ruleset.functions());
        assertEquals(1, ruleset.rules().size());
        assertEquals("http://docs", ruleset.documentationUrl());
    }
}
