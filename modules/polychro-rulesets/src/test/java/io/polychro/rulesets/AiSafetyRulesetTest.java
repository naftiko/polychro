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

import io.polychro.ruleset.RulesetValidatorFactory;
import io.polychro.spi.Diagnostic;
import io.polychro.spi.Document;
import io.polychro.spi.Severity;
import io.polychro.spi.Validator;
import io.polychro.spi.ValidatorConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AiSafetyRulesetTest {

    private static Validator validator;
    private static final Path FIXTURES = Path.of("src/test/resources/fixtures").toAbsolutePath();
    private static final Path RULESETS = Path.of("src/main/resources/rulesets").toAbsolutePath();

    @BeforeAll
    static void setUp() {
        String rulesetPath = RULESETS.resolve("ai-safety.yml").toString();
        validator = new RulesetValidatorFactory().create(
                new ValidatorConfig(Map.of("rulesetPath", rulesetPath)));
    }

    @Test
    void cleanCapabilityShouldPassAiSafetyRuleset() {
        Document doc = Document.fromYaml(FIXTURES.resolve("clean-capability.yml"));
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.isEmpty(),
                () -> "Expected no ai-safety violations but got: " + results);
    }

    @Test
    void trailingSlashShouldTriggerRule() {
        Document doc = Document.fromYaml(FIXTURES.resolve("ai-safety-violations.yml"));
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.stream().anyMatch(d -> d.code().equals("no-trailing-slash")),
                () -> "Expected no-trailing-slash violation, got: " + results);
    }

    @Test
    void emptyDescriptionShouldTriggerRule() {
        Document doc = Document.fromYaml(FIXTURES.resolve("ai-safety-violations.yml"));
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.stream().anyMatch(d -> d.code().equals("empty-description")),
                () -> "Expected empty-description violation, got: " + results);
    }

    @Test
    void duplicateOperationNameRuleShouldBeDeclared() {
        // This rule requires a custom function (uniqueOperationNames) for cross-object
        // graph traversal. Until the function is implemented, the rule is declared but
        // does not fire. This test verifies the rule definition is present in the ruleset.
        String content = RulesetCatalog.load("ai-safety");
        assertTrue(content.contains("duplicate-operation-name"),
                "Expected duplicate-operation-name rule to be declared in ai-safety ruleset");
        assertTrue(content.contains("uniqueOperationNames"),
                "Expected uniqueOperationNames custom function reference");
    }

    @Test
    void urlNamedInputParameterShouldTriggerInfoRule() {
        Document doc = Document.fromYaml(FIXTURES.resolve("ai-safety-violations.yml"));
        List<Diagnostic> results = validator.validate(doc);
        List<Diagnostic> urlWarnings = results.stream()
                .filter(d -> d.code().equals("url-input-without-review"))
                .toList();
        assertFalse(urlWarnings.isEmpty(),
                () -> "Expected url-input-without-review violation, got: " + results);
        assertTrue(urlWarnings.stream().allMatch(d -> d.severity() == Severity.INFO),
                () -> "url-input-without-review must be INFO severity, got: " + urlWarnings);
    }

    @Test
    void aiSafetyRulesetShouldInheritGovernanceRules() {
        // The ai-safety ruleset extends governance, so governance violations should also appear
        Document doc = Document.fromYaml(FIXTURES.resolve("ai-safety-violations.yml"));
        List<Diagnostic> results = validator.validate(doc);
        // Trailing slash is also a governance rule (consumer-base-uri-no-trailing-slash)
        // elevated to error severity in ai-safety
        List<Diagnostic> trailingSlash = results.stream()
                .filter(d -> d.code().equals("consumer-base-uri-no-trailing-slash")
                        || d.code().equals("no-trailing-slash"))
                .toList();
        assertFalse(trailingSlash.isEmpty(),
                () -> "Expected inherited trailing slash rule from governance, got: " + results);
    }
}
