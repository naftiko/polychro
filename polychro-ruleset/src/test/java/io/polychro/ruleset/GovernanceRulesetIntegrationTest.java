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

import io.polychro.spi.Diagnostic;
import io.polychro.spi.Document;
import io.polychro.spi.ValidatorConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the governance ruleset against real YAML documents.
 * Each test describes a specific rule behaviour and guards against regressions.
 */
class GovernanceRulesetIntegrationTest {

    private RulesetValidator validator;

    @BeforeEach
    void setUp() {
        String rulesetPath = Path.of("../polychro-rulesets/src/main/resources/rulesets/governance.yml")
                .toAbsolutePath().toString();
        RulesetValidatorFactory factory = new RulesetValidatorFactory();
        validator = (RulesetValidator) factory.create(
                new ValidatorConfig(Map.of("rulesetPath", rulesetPath)));
    }

    // ── capability-version-format ────────────────────────────────────────────

    @Test
    void capabilityVersionFormatShouldFireWhenVersionIsNotSemver() {
        Document doc = Document.fromString(
                "info:\n  name: my-capability\n  version: not-semver\n", "yaml");
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(
                results.stream().anyMatch(d -> d.code().equals("capability-version-format")),
                "Expected capability-version-format diagnostic but got: " + results);
    }

    @Test
    void capabilityVersionFormatShouldPassWhenVersionIsSemver() {
        Document doc = Document.fromString(
                "info:\n  name: my-capability\n  version: 1.0.0\n", "yaml");
        List<Diagnostic> results = validator.validate(doc);
        assertFalse(
                results.stream().anyMatch(d -> d.code().equals("capability-version-format")),
                "capability-version-format should not fire on valid semver but got: " + results);
    }

    // ── capability-name-present ──────────────────────────────────────────────

    @Test
    void capabilityNamePresentShouldFireWhenInfoIsMissing() {
        Document doc = Document.fromString("kind: capability\n", "yaml");
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(
                results.stream().anyMatch(d -> d.code().equals("capability-name-present")),
                "Expected capability-name-present diagnostic but got: " + results);
    }

    @Test
    void capabilityNamePresentShouldPassWhenInfoIsPresent() {
        Document doc = Document.fromString(
                "info:\n  name: my-capability\n  version: 1.0.0\n", "yaml");
        List<Diagnostic> results = validator.validate(doc);
        assertFalse(
                results.stream().anyMatch(d -> d.code().equals("capability-name-present")),
                "capability-name-present should not fire when info is present but got: " + results);
    }
}
