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

import io.polychro.ruleset.Ruleset;
import io.polychro.ruleset.utils.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RulesetCatalogTest {

    @Test
    void availableShouldReturnAllRulesetNames() {
        List<String> names = RulesetCatalog.available();
        assertEquals(8, names.size());
        assertTrue(names.contains("governance"));
        assertTrue(names.contains("ai-safety"));
        assertTrue(names.contains("security"));
        assertTrue(names.contains("mcp"));
        assertTrue(names.contains("consistency"));
        assertTrue(names.contains("resilience"));
        assertTrue(names.contains("agents"));
        assertTrue(names.contains("openapi"));
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
    void loadShouldReturnResilienceContent() {
        String content = RulesetCatalog.load("resilience");
        assertNotNull(content);
        assertTrue(content.contains("consumer-timeout-declared"));
        assertTrue(content.contains("recommended: false"));
    }

    @Test
    void loadShouldReturnOpenapiContent() {
        String content = RulesetCatalog.load("openapi");
        assertNotNull(content);
        assertTrue(content.contains("openapi-paths-kebab-case"));
    }

    @Test
    void loadShouldThrowForUnknownRuleset() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> RulesetCatalog.load("nonexistent"));
        assertTrue(ex.getMessage().contains("Unknown ruleset: nonexistent"));
        assertTrue(ex.getMessage().contains("Available:"));
    }

    @ParameterizedTest
    @CsvSource(value = {"governance,capability-name-present", "ai-safety,consumer-base-uri-no-trailing-slash",
            "security,no-hardcoded-secrets", "mcp,mcp-tool-description-present",
            "consistency,naming-convention-kebab", "resilience,consumer-timeout-declared",
            "openapi,openapi-paths-kebab-case"})
    void loadAsRulesetShouldReturnContent(String rulesetName, String ruleName) {
        Ruleset ruleset = RulesetCatalog.loadAsRuleset(rulesetName);
        assertNotNull(ruleset);
        assertTrue(ruleset.rules().containsKey(ruleName));
    }

    @Test
    void loadAsRulesetShouldReturnAgentsContent() {
        Ruleset ruleset = RulesetCatalog.loadAsRuleset("agents");
        assertNotNull(ruleset);
        assertTrue(ruleset.overrides().getFirst().rules().containsKey("agents-required-sections"));
    }

    @Test
    void loadAsRulesetShouldThrowForUnknownRuleset() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> RulesetCatalog.loadAsRuleset("nonexistent"));
        assertTrue(ex.getMessage().contains("Unknown ruleset: nonexistent"));
        assertTrue(ex.getMessage().contains("Available:"));
    }

    @Test
    void loadShouldThrowOnError() {
        try (var mockStatic = Mockito.mockStatic(FileUtils.class)) {
            mockStatic.when(() -> FileUtils.getFileContentFromClasspath(Mockito.anyString())).thenThrow(new IOException("Error"));
            assertThrows(UncheckedIOException.class, () -> RulesetCatalog.load("agents"));
        }
    }
}
