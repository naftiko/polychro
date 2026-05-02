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
package io.polychro.rulesets;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RulesetCatalogTest {

    @Test
    void availableShouldReturnAllRulesetNames() {
        List<String> names = RulesetCatalog.available();
        assertEquals(5, names.size());
        assertTrue(names.contains("governance"));
        assertTrue(names.contains("ai-safety"));
        assertTrue(names.contains("security"));
        assertTrue(names.contains("mcp"));
        assertTrue(names.contains("consistency"));
    }

    @Test
    void loadShouldReturnGovernanceContent() {
        String content = RulesetCatalog.load("governance");
        assertNotNull(content);
        assertTrue(content.contains("capability-name-present"));
    }

    @Test
    void loadShouldReturnAiSafetyContent() {
        String content = RulesetCatalog.load("ai-safety");
        assertNotNull(content);
        assertTrue(content.contains("extends:"));
    }

    @Test
    void loadShouldReturnSecurityContent() {
        String content = RulesetCatalog.load("security");
        assertNotNull(content);
        assertTrue(content.contains("no-hardcoded-secrets"));
    }

    @Test
    void loadShouldReturnMcpContent() {
        String content = RulesetCatalog.load("mcp");
        assertNotNull(content);
        assertTrue(content.contains("mcp-tool-description-present"));
    }

    @Test
    void loadShouldReturnConsistencyContent() {
        String content = RulesetCatalog.load("consistency");
        assertNotNull(content);
        assertTrue(content.contains("naming-convention-kebab"));
    }

    @Test
    void loadShouldThrowForUnknownRuleset() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> RulesetCatalog.load("nonexistent"));
        assertTrue(ex.getMessage().contains("Unknown ruleset: nonexistent"));
        assertTrue(ex.getMessage().contains("Available:"));
    }

    @Test
    void resourcePathShouldReturnCorrectPath() {
        assertEquals("/rulesets/governance.yml", RulesetCatalog.resourcePath("governance"));
        assertEquals("/rulesets/ai-safety.yml", RulesetCatalog.resourcePath("ai-safety"));
        assertEquals("/rulesets/security.yml", RulesetCatalog.resourcePath("security"));
        assertEquals("/rulesets/mcp.yml", RulesetCatalog.resourcePath("mcp"));
        assertEquals("/rulesets/consistency.yml", RulesetCatalog.resourcePath("consistency"));
    }

    @Test
    void resourcePathShouldThrowForUnknownRuleset() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> RulesetCatalog.resourcePath("invalid"));
        assertTrue(ex.getMessage().contains("Unknown ruleset: invalid"));
    }
}
