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

class ResilienceRulesetTest {

    private static Validator validator;
    private static final Path FIXTURES = Path.of("src/test/resources/fixtures").toAbsolutePath();

    @BeforeAll
    static void setUp() {
        String content = RulesetCatalog.load("resilience");
        validator = new RulesetValidatorFactory().create(
                new ValidatorConfig(Map.of(
                        "rulesetContent", content,
                        "includeNonRecommended", true)));
    }

    @Test
    void cleanCapabilityShouldPassResilienceRuleset() {
        Document doc = Document.fromYaml(FIXTURES.resolve("clean-capability.yml"));
        List<Diagnostic> results = validator.validate(doc);
        // Clean capability may still trigger resilience rules (no timeout, no retry)
        // but they should all be INFO severity
        assertTrue(results.stream().allMatch(d -> d.severity() == Severity.INFO),
                () -> "Expected only INFO-level diagnostics, got: " + results);
    }

    @Test
    void missingTimeoutShouldTriggerInfoDiagnostic() {
        Document doc = Document.fromYaml(FIXTURES.resolve("resilience-violations.yml"));
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.stream().anyMatch(d -> d.code().equals("consumer-timeout-declared")),
                () -> "Expected consumer-timeout-declared violation, got: " + results);
    }

    @Test
    void missingRetryPolicyShouldTriggerInfoDiagnostic() {
        Document doc = Document.fromYaml(FIXTURES.resolve("resilience-violations.yml"));
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.stream().anyMatch(d -> d.code().equals("consumer-retry-policy-present")),
                () -> "Expected consumer-retry-policy-present violation, got: " + results);
    }

    @Test
    void unboundedArrayOutputShouldTriggerInfoDiagnostic() {
        Document doc = Document.fromYaml(FIXTURES.resolve("resilience-violations.yml"));
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.stream().anyMatch(d -> d.code().equals("array-output-unbounded")),
                () -> "Expected array-output-unbounded violation, got: " + results);
    }

    @Test
    void unconstrainedStringInputShouldTriggerInfoDiagnostic() {
        Document doc = Document.fromYaml(FIXTURES.resolve("resilience-violations.yml"));
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.stream().anyMatch(d -> d.code().equals("string-input-unconstrained")),
                () -> "Expected string-input-unconstrained violation, got: " + results);
    }

    @Test
    void missingSemanticsOnAggregateShouldTriggerInfoDiagnostic() {
        Document doc = Document.fromYaml(FIXTURES.resolve("resilience-violations.yml"));
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.stream().anyMatch(d -> d.code().equals("semantics-declared-on-write-operations")),
                () -> "Expected semantics-declared-on-write-operations violation, got: " + results);
    }

    @Test
    void allResilienceViolationsShouldBeInfoSeverity() {
        Document doc = Document.fromYaml(FIXTURES.resolve("resilience-violations.yml"));
        List<Diagnostic> results = validator.validate(doc);
        assertFalse(results.isEmpty(), "Expected at least one resilience diagnostic");
        assertTrue(results.stream().allMatch(d -> d.severity() == Severity.INFO),
                () -> "All resilience rules must be INFO severity, got: " + results);
    }

    @Test
    void rulesetShouldNotBeRecommended() {
        String content = RulesetCatalog.load("resilience");
        assertTrue(content.contains("recommended: false"),
                "Resilience ruleset must be marked as recommended: false");
    }
}
