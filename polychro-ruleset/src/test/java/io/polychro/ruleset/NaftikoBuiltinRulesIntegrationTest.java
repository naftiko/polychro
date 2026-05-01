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

class NaftikoBuiltinRulesIntegrationTest {

    private RulesetValidator validator;
    private RulesetValidator validatorWithOptional;

    @BeforeEach
    void setUp() {
        String rulesetPath = Path.of("src/test/resources/fixtures/naftiko-builtin-rules.yml")
                .toAbsolutePath().toString();
        RulesetValidatorFactory factory = new RulesetValidatorFactory();

        validator = (RulesetValidator) factory.create(
                new ValidatorConfig(Map.of("rulesetPath", rulesetPath)));
        validatorWithOptional = (RulesetValidator) factory.create(
                new ValidatorConfig(Map.of("rulesetPath", rulesetPath, "includeNonRecommended", true)));
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
    void trailingSlashBaseUriShouldProduceDiagnostic() {
        Document doc = Document.fromYaml(
                Path.of("src/test/resources/fixtures/trailing-slash-baseuri.yml").toAbsolutePath());
        List<Diagnostic> results = validator.validate(doc);
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(d ->
                d.code().equals("naftiko-consumes-baseuri-no-trailing-slash")));
    }

    @Test
    void queryInPathShouldProduceDiagnostic() {
        Document doc = Document.fromYaml(
                Path.of("src/test/resources/fixtures/query-in-path.yml").toAbsolutePath());
        List<Diagnostic> results = validator.validate(doc);
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(d ->
                d.code().equals("naftiko-consumed-resource-no-query-in-path")));
    }

    @Test
    void scriptTagInDescriptionShouldProduceErrorDiagnostic() {
        Document doc = Document.fromYaml(
                Path.of("src/test/resources/fixtures/script-tag-in-description.yml").toAbsolutePath());
        List<Diagnostic> results = validator.validate(doc);
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(d ->
                d.code().equals("naftiko-no-script-tags-in-markdown")
                        && d.severity() == Severity.ERROR));
    }

    @Test
    void evalInDescriptionShouldProduceErrorDiagnostic() {
        Document doc = Document.fromYaml(
                Path.of("src/test/resources/fixtures/eval-in-description.yml").toAbsolutePath());
        List<Diagnostic> results = validator.validate(doc);
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(d ->
                d.code().equals("naftiko-no-eval-in-markdown")
                        && d.severity() == Severity.ERROR));
    }

    @Test
    void exampleComBaseUriShouldProduceDiagnosticWhenOptionalEnabled() {
        Document doc = Document.fromYaml(
                Path.of("src/test/resources/fixtures/example-com-baseuri.yml").toAbsolutePath());

        // Without optional rules — should not fire (recommended=false)
        List<Diagnostic> resultsDefault = validator.validate(doc);
        assertTrue(resultsDefault.stream().noneMatch(d ->
                d.code().equals("naftiko-baseuri-not-example")));

        // With optional rules — should fire
        List<Diagnostic> resultsOptional = validatorWithOptional.validate(doc);
        assertTrue(resultsOptional.stream().anyMatch(d ->
                d.code().equals("naftiko-baseuri-not-example")));
    }

    @Test
    void missingResourceDescriptionShouldProduceDiagnostic() {
        Document doc = Document.fromYaml(
                Path.of("src/test/resources/fixtures/missing-resource-description.yml").toAbsolutePath());
        List<Diagnostic> results = validator.validate(doc);
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(d ->
                d.code().equals("naftiko-rest-resource-description")));
    }
}
