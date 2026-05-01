package io.polychro.ruleset;

import io.polychro.spi.Diagnostic;
import io.polychro.spi.Document;
import io.polychro.spi.ValidatorConfig;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OverridesScopedAliasTest {

    private static final Path OVERRIDES_DIR = Path.of("src/test/resources/fixtures/overrides").toAbsolutePath();

    @Test
    void aliasDeclaredInsideOverrideShouldBeResolvable() {
        String rulesetPath = OVERRIDES_DIR.resolve("ruleset-with-scoped-aliases.yml").toString();
        RulesetValidatorFactory factory = new RulesetValidatorFactory();
        RulesetValidator validator = (RulesetValidator) factory.create(
                new ValidatorConfig(Map.of("rulesetPath", rulesetPath)));

        // Document that matches "schemas/**" — SchemaRoot alias should work
        Document doc = Document.fromString(
                "{\"info\": {\"name\": \"test\"}}",
                "json", "schemas/model.json");

        List<Diagnostic> results = validator.validate(doc);

        // schema-rule uses #SchemaRoot alias — should fire because $.definitions is undefined
        assertTrue(results.stream().anyMatch(d -> d.code().equals("schema-rule")));
    }

    @Test
    void aliasFromSiblingOverrideShouldNotBeResolvable() {
        // When document doesn't match the override with SchemaRoot alias,
        // the alias should not be available
        String rulesetPath = OVERRIDES_DIR.resolve("ruleset-with-scoped-aliases.yml").toString();
        RulesetValidatorFactory factory = new RulesetValidatorFactory();
        RulesetValidator validator = (RulesetValidator) factory.create(
                new ValidatorConfig(Map.of("rulesetPath", rulesetPath)));

        // Document that does NOT match "schemas/**"
        Document doc = Document.fromString(
                "{\"info\": {\"name\": \"test\"}}",
                "json", "api/spec.json");

        List<Diagnostic> results = validator.validate(doc);

        // schema-rule should NOT be in the rules for this document path
        assertFalse(results.stream().anyMatch(d -> d.code().equals("schema-rule")));
    }

    @Test
    void rootLevelAliasShouldBeResolvableInsideOverride() {
        String rulesetPath = OVERRIDES_DIR.resolve("ruleset-with-scoped-aliases.yml").toString();
        RulesetValidatorFactory factory = new RulesetValidatorFactory();
        RulesetValidator validator = (RulesetValidator) factory.create(
                new ValidatorConfig(Map.of("rulesetPath", rulesetPath)));

        // Root-level alias #InfoObject is used by name-truthy
        // It should resolve for any document path
        Document doc = Document.fromString(
                "{\"info\": {\"name\": \"\"}}",
                "json", "schemas/model.json");

        List<Diagnostic> results = validator.validate(doc);

        // name-truthy uses #InfoObject.name — should fire
        assertTrue(results.stream().anyMatch(d -> d.code().equals("name-truthy")));
    }
}
