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
import io.polychro.spi.Validator;
import io.polychro.spi.ValidatorConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConsistencyRulesetTest {

    private static Validator validator;
    private static final Path FIXTURES = Path.of("src/test/resources/fixtures").toAbsolutePath();

    @BeforeAll
    static void setUp() {
        String content = RulesetCatalog.load("consistency");
        validator = new RulesetValidatorFactory().create(
                new ValidatorConfig(Map.of("rulesetContent", content)));
    }

    @Test
    void cleanCapabilityShouldPassConsistencyRuleset() {
        Document doc = Document.fromYaml(FIXTURES.resolve("clean-capability.yml"));
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.isEmpty(),
                () -> "Expected no consistency violations but got: " + results);
    }

    @Test
    void nonKebabNamespaceShouldTriggerRule() {
        Document doc = Document.fromYaml(FIXTURES.resolve("consistency-violations.yml"));
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.stream().anyMatch(d -> d.code().equals("naming-convention-kebab")),
                () -> "Expected naming-convention-kebab violation, got: " + results);
    }

    @Test
    void queryInPathShouldTriggerRule() {
        Document doc = Document.fromYaml(FIXTURES.resolve("consistency-violations.yml"));
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.stream().anyMatch(d -> d.code().equals("resource-path-no-query")),
                () -> "Expected resource-path-no-query violation, got: " + results);
    }

    @Test
    void missingLeadingSlashShouldTriggerRule() {
        Document doc = Document.fromYaml(FIXTURES.resolve("consistency-violations.yml"));
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.stream().anyMatch(d -> d.code().equals("resource-path-leading-slash")),
                () -> "Expected resource-path-leading-slash violation, got: " + results);
    }
}
