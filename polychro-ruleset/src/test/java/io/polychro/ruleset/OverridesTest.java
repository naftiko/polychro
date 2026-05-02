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
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OverridesTest {

    private static final Path OVERRIDES_DIR = Path.of("src/test/resources/fixtures/overrides").toAbsolutePath();

    @Test
    void fileGlobMatchShouldDisableRule() {
        String rulesetPath = OVERRIDES_DIR.resolve("ruleset-with-overrides.yml").toString();
        RulesetValidatorFactory factory = new RulesetValidatorFactory();
        RulesetValidator validator = (RulesetValidator) factory.create(
                new ValidatorConfig(Map.of("rulesetPath", rulesetPath)));

        // Document that matches "schemas/**/*.json" — name-truthy should be off
        Document doc = Document.fromString(
                "{\"info\": {\"name\": \"\", \"description\": \"present\"}}",
                "json", "schemas/user/model.json");

        List<Diagnostic> results = validator.validate(doc);

        // name-truthy is disabled for schemas/**/*.json
        assertFalse(results.stream().anyMatch(d -> d.code().equals("name-truthy")));
        // schema-type-check should be active
        assertTrue(results.stream().anyMatch(d -> d.code().equals("schema-type-check")));
    }

    @Test
    void fileGlobNoMatchShouldKeepRuleActive() {
        String rulesetPath = OVERRIDES_DIR.resolve("ruleset-with-overrides.yml").toString();
        RulesetValidatorFactory factory = new RulesetValidatorFactory();
        RulesetValidator validator = (RulesetValidator) factory.create(
                new ValidatorConfig(Map.of("rulesetPath", rulesetPath)));

        // Document that does NOT match "schemas/**/*.json"
        Document doc = Document.fromString(
                "{\"info\": {\"name\": \"\", \"description\": \"present\"}}",
                "json", "api/spec.json");

        List<Diagnostic> results = validator.validate(doc);

        // name-truthy should still be active
        assertTrue(results.stream().anyMatch(d -> d.code().equals("name-truthy")));
        // schema-type-check should NOT be active (only in schemas/** override)
        assertFalse(results.stream().anyMatch(d -> d.code().equals("schema-type-check")));
    }

    @Test
    void overrideShouldChangeSeverityPerFile() {
        String rulesetPath = OVERRIDES_DIR.resolve("ruleset-with-overrides.yml").toString();
        RulesetValidatorFactory factory = new RulesetValidatorFactory();
        RulesetValidator validator = (RulesetValidator) factory.create(
                new ValidatorConfig(Map.of("rulesetPath", rulesetPath)));

        // Document that matches "tests/**" — desc-truthy severity changes to info
        Document doc = Document.fromString(
                "{\"info\": {\"name\": \"test\", \"description\": \"\"}}",
                "json", "tests/unit/test-file.json");

        List<Diagnostic> results = validator.validate(doc);

        assertTrue(results.stream().anyMatch(d ->
                d.code().equals("desc-truthy") && d.severity() == io.polychro.spi.Severity.INFO));
    }

    @Test
    void newRuleAddedInOverrideShouldBeActive() {
        String rulesetPath = OVERRIDES_DIR.resolve("ruleset-with-overrides.yml").toString();
        RulesetValidatorFactory factory = new RulesetValidatorFactory();
        RulesetValidator validator = (RulesetValidator) factory.create(
                new ValidatorConfig(Map.of("rulesetPath", rulesetPath)));

        // Document that matches "schemas/**/*.json"
        Document doc = Document.fromString(
                "{\"info\": {\"name\": \"\"}}",
                "json", "schemas/pet/schema.json");

        List<Diagnostic> results = validator.validate(doc);

        // schema-type-check is added via override
        assertTrue(results.stream().anyMatch(d -> d.code().equals("schema-type-check")));
    }

    @Test
    void noSourcePathShouldNotApplyOverrides() {
        String rulesetPath = OVERRIDES_DIR.resolve("ruleset-with-overrides.yml").toString();
        RulesetValidatorFactory factory = new RulesetValidatorFactory();
        RulesetValidator validator = (RulesetValidator) factory.create(
                new ValidatorConfig(Map.of("rulesetPath", rulesetPath)));

        // Document without source path — overrides should not apply
        Document doc = Document.fromString(
                "{\"info\": {\"name\": \"\", \"description\": \"\"}}",
                "json");

        List<Diagnostic> results = validator.validate(doc);

        // Base rules should apply
        assertTrue(results.stream().anyMatch(d -> d.code().equals("name-truthy")));
        assertTrue(results.stream().anyMatch(d -> d.code().equals("desc-truthy")));
    }
}
