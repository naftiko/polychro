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
