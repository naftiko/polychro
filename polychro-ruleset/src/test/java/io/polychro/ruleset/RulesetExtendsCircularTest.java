package io.polychro.ruleset;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;

import io.polychro.spi.ValidatorConfig;

import static org.junit.jupiter.api.Assertions.*;

class RulesetExtendsCircularTest {

    private static final Path EXTENDS_DIR = Path.of("src/test/resources/fixtures/extends").toAbsolutePath();

    @Test
    void circularExtendsShouldThrowWithCyclePath() {
        String rulesetPath = EXTENDS_DIR.resolve("circular-a.yml").toString();
        RulesetValidatorFactory factory = new RulesetValidatorFactory();

        RulesetParseException exception = assertThrows(RulesetParseException.class, () ->
                factory.create(new ValidatorConfig(Map.of("rulesetPath", rulesetPath))));

        assertTrue(exception.getMessage().contains("Circular extends detected"),
                "Expected circular message but got: " + exception.getMessage());
    }

    @Test
    void selfReferenceShouldThrowWithCyclePath() {
        String rulesetPath = EXTENDS_DIR.resolve("self-reference.yml").toString();
        RulesetValidatorFactory factory = new RulesetValidatorFactory();

        RulesetParseException exception = assertThrows(RulesetParseException.class, () ->
                factory.create(new ValidatorConfig(Map.of("rulesetPath", rulesetPath))));

        assertTrue(exception.getMessage().contains("Circular extends detected"),
                "Expected circular message but got: " + exception.getMessage());
    }

    @Test
    void threeNodeCycleShouldThrowWithCyclePath() {
        String rulesetPath = EXTENDS_DIR.resolve("cycle-3a.yml").toString();
        RulesetValidatorFactory factory = new RulesetValidatorFactory();

        RulesetParseException exception = assertThrows(RulesetParseException.class, () ->
                factory.create(new ValidatorConfig(Map.of("rulesetPath", rulesetPath))));

        assertTrue(exception.getMessage().contains("Circular extends detected"),
                "Expected circular message but got: " + exception.getMessage());
        // Should contain the cycle path
        assertTrue(exception.getMessage().contains("->"),
                "Expected cycle path in message: " + exception.getMessage());
    }
}
