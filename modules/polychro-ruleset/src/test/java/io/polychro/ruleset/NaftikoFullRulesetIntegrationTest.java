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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NaftikoFullRulesetIntegrationTest {

    private RulesetValidator validator;

    @BeforeEach
    void setUp() {
        String rulesetPath = Path.of("src/test/resources/fixtures/naftiko-full-rules.yml")
                .toAbsolutePath().toString();
        RulesetValidatorFactory factory = new RulesetValidatorFactory();
        validator = (RulesetValidator) factory.create(
                new ValidatorConfig(Map.of("rulesetPath", rulesetPath)));
    }

    @Test
    void allRulesShouldParseSuccessfully() {
        // The ruleset has 17 rules — just loading it is the test
        assertNotNull(validator);
    }

    @Test
    void validCapabilityShouldProduceZeroDiagnostics() {
        Document doc = Document.fromYaml(
                Path.of("src/test/resources/fixtures/valid-capability.yml").toAbsolutePath());
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.isEmpty(),
                "Valid capability should produce no diagnostics but got: " + results);
    }

    @Test
    void trailingSlashShouldFireViaAlias() {
        // The full ruleset uses #Consumes.baseUri alias
        Document doc = Document.fromYaml(
                Path.of("src/test/resources/fixtures/trailing-slash-baseuri.yml").toAbsolutePath());
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.stream().anyMatch(d ->
                d.code().equals("naftiko-consumes-baseuri-no-trailing-slash")));
    }

    @Test
    void scriptTagShouldFireViaAlias() {
        Document doc = Document.fromYaml(
                Path.of("src/test/resources/fixtures/script-tag-in-description.yml").toAbsolutePath());
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.stream().anyMatch(d ->
                d.code().equals("naftiko-no-script-tags-in-markdown")
                        && d.severity() == Severity.ERROR));
    }

    @Test
    void validCapabilityShouldPassKebabCaseCheck() {
        Document doc = Document.fromYaml(
                Path.of("src/test/resources/fixtures/valid-capability.yml").toAbsolutePath());
        List<Diagnostic> results = validator.validate(doc);
        assertFalse(results.stream().anyMatch(d ->
                d.code().equals("naftiko-info-name-kebab")));
    }

    @Test
    void validCapabilityShouldPassTypeEnumCheck() {
        Document doc = Document.fromYaml(
                Path.of("src/test/resources/fixtures/valid-capability.yml").toAbsolutePath());
        List<Diagnostic> results = validator.validate(doc);
        assertFalse(results.stream().anyMatch(d ->
                d.code().equals("naftiko-consumes-type-enum")));
        assertFalse(results.stream().anyMatch(d ->
                d.code().equals("naftiko-exposes-type-enum")));
    }

    @Test
    void emptyDocumentShouldFireInfoDefinedAndSpecVersionDefined() {
        Document doc = Document.fromString("{}", "json");
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.stream().anyMatch(d ->
                d.code().equals("naftiko-info-version-defined") && d.severity() == Severity.ERROR));
        assertTrue(results.stream().anyMatch(d ->
                d.code().equals("naftiko-spec-version-defined") && d.severity() == Severity.ERROR));
    }
}
