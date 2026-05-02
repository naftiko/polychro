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

class GovernanceRulesetTest {

    private static Validator validator;
    private static final Path FIXTURES = Path.of("src/test/resources/fixtures").toAbsolutePath();

    @BeforeAll
    static void setUp() {
        String content = RulesetCatalog.load("governance");
        validator = new RulesetValidatorFactory().create(
                new ValidatorConfig(Map.of("rulesetContent", content)));
    }

    @Test
    void cleanCapabilityShouldPassWithNoDiagnostics() {
        Document doc = Document.fromYaml(FIXTURES.resolve("clean-capability.yml"));
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.isEmpty(),
                () -> "Expected no violations but got: " + results);
    }

    @Test
    void missingTagsShouldTriggerCapabilityTagsPresent() {
        Document doc = Document.fromYaml(FIXTURES.resolve("governance-violations.yml"));
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.stream().anyMatch(d -> d.code().equals("capability-tags-present")),
                () -> "Expected capability-tags-present violation, got: " + results);
    }

    @Test
    void trailingSlashShouldTriggerRule() {
        Document doc = Document.fromYaml(FIXTURES.resolve("governance-violations.yml"));
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.stream().anyMatch(d -> d.code().equals("consumer-base-uri-no-trailing-slash")),
                () -> "Expected trailing slash violation, got: " + results);
    }

    @Test
    void missingConsumerDescriptionShouldTriggerRule() {
        Document doc = Document.fromYaml(FIXTURES.resolve("governance-violations.yml"));
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.stream().anyMatch(d -> d.code().equals("consumer-description-present")),
                () -> "Expected consumer-description-present violation, got: " + results);
    }

    @Test
    void missingExposeDescriptionShouldTriggerRule() {
        Document doc = Document.fromYaml(FIXTURES.resolve("governance-violations.yml"));
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.stream().anyMatch(d -> d.code().equals("expose-description-present")),
                () -> "Expected expose-description-present violation, got: " + results);
    }

    @Test
    void nonKebabExposeNamespaceShouldTriggerRule() {
        Document doc = Document.fromYaml(FIXTURES.resolve("governance-violations.yml"));
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.stream().anyMatch(d -> d.code().equals("expose-namespace-kebab-case")),
                () -> "Expected expose-namespace-kebab-case violation, got: " + results);
    }
}
