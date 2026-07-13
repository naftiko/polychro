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
import io.polychro.spi.Severity;
import io.polychro.spi.ValidatorConfig;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RulesetExtendsTest {

    private static final Path EXTENDS_DIR = Path.of("src/test/resources/fixtures/extends").toAbsolutePath();

    @Test
    void singleLevelExtendsShouldInheritParentRules() {
        String rulesetPath = EXTENDS_DIR.resolve("child-ruleset.yml").toString();
        RulesetValidatorFactory factory = new RulesetValidatorFactory();
        RulesetValidator validator = (RulesetValidator) factory.create(
                new ValidatorConfig(Map.of("rulesetPath", rulesetPath)));

        // Document with empty name, empty description, empty version — should fire all 3 rules
        Document doc = Document.fromString(
                "{\"info\": {\"name\": \"\", \"description\": \"\", \"version\": \"\"}}", "json");
        List<Diagnostic> results = validator.validate(doc);

        // Should have diagnostics from parent (name-truthy, desc-truthy) and child (version-truthy)
        assertTrue(results.stream().anyMatch(d -> d.code().equals("parent-name-truthy")));
        assertTrue(results.stream().anyMatch(d -> d.code().equals("parent-desc-truthy")));
        assertTrue(results.stream().anyMatch(d -> d.code().equals("child-version-truthy")));
    }

    @Test
    void twoLevelExtendsShouldInheritGrandparentRules() {
        String rulesetPath = EXTENDS_DIR.resolve("two-level-child.yml").toString();
        RulesetValidatorFactory factory = new RulesetValidatorFactory();
        RulesetValidator validator = (RulesetValidator) factory.create(
                new ValidatorConfig(Map.of("rulesetPath", rulesetPath)));

        // Document with missing info — should fire grandparent's rule
        Document doc = Document.fromString("{\"name\": \"test\"}", "json");
        List<Diagnostic> results = validator.validate(doc);

        // Grandparent rule checks for $.info field on root
        assertTrue(results.stream().anyMatch(d -> d.code().equals("grandparent-rule")));
        // Middle rule — name will be missing, so truthy on $.info.name won't match (no node)
        // Child rule — version will be missing
    }

    @Test
    void childShouldOverrideParentRuleSeverity() {
        String rulesetPath = EXTENDS_DIR.resolve("override-severity-child.yml").toString();
        RulesetValidatorFactory factory = new RulesetValidatorFactory();
        RulesetValidator validator = (RulesetValidator) factory.create(
                new ValidatorConfig(Map.of("rulesetPath", rulesetPath)));

        // Child overrides parent-name-truthy severity from warn to error
        Document doc = Document.fromString(
                "{\"info\": {\"name\": \"\", \"description\": \"present\"}}", "json");
        List<Diagnostic> results = validator.validate(doc);

        assertTrue(results.stream().anyMatch(d ->
                d.code().equals("parent-name-truthy") && d.severity() == Severity.ERROR));
    }

    @Test
    void childShouldOverrideParentRuleMessage() {
        String rulesetPath = EXTENDS_DIR.resolve("override-severity-child.yml").toString();
        RulesetValidatorFactory factory = new RulesetValidatorFactory();
        RulesetValidator validator = (RulesetValidator) factory.create(
                new ValidatorConfig(Map.of("rulesetPath", rulesetPath)));

        Document doc = Document.fromString(
                "{\"info\": {\"name\": \"\", \"description\": \"present\"}}", "json");
        List<Diagnostic> results = validator.validate(doc);

        assertTrue(results.stream().anyMatch(d ->
                d.code().equals("parent-name-truthy") && d.message().equals("Name is mandatory")));
    }

    @Test
    void offSeverityShouldDisableInheritedRule() {
        String rulesetPath = EXTENDS_DIR.resolve("override-severity-child.yml").toString();
        RulesetValidatorFactory factory = new RulesetValidatorFactory();
        RulesetValidator validator = (RulesetValidator) factory.create(
                new ValidatorConfig(Map.of("rulesetPath", rulesetPath)));

        Document doc = Document.fromString(
                "{\"info\": {\"name\": \"\", \"description\": \"\"}}", "json");
        List<Diagnostic> results = validator.validate(doc);

        // parent-desc-truthy is turned off — should not fire
        assertFalse(results.stream().anyMatch(d -> d.code().equals("parent-desc-truthy")));
    }

    @Test
    void childShouldAddNewRulesAlongsideInherited() {
        String rulesetPath = EXTENDS_DIR.resolve("child-ruleset.yml").toString();
        RulesetValidatorFactory factory = new RulesetValidatorFactory();
        RulesetValidator validator = (RulesetValidator) factory.create(
                new ValidatorConfig(Map.of("rulesetPath", rulesetPath)));

        Document doc = Document.fromString(
                "{\"info\": {\"name\": \"test\", \"description\": \"ok\", \"version\": \"\"}}", "json");
        List<Diagnostic> results = validator.validate(doc);

        // Only child's rule should fire (name and description are truthy)
        assertTrue(results.stream().anyMatch(d -> d.code().equals("child-version-truthy")));
        assertFalse(results.stream().anyMatch(d -> d.code().equals("parent-name-truthy")));
        assertFalse(results.stream().anyMatch(d -> d.code().equals("parent-desc-truthy")));
    }
}
