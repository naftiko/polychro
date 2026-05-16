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

import io.polychro.spi.Validator;
import io.polychro.spi.ValidatorConfig;
import io.polychro.spi.ValidatorFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RulesetValidatorFactoryTest {

    @Test
    void nameShouldReturnRuleset() {
        RulesetValidatorFactory factory = new RulesetValidatorFactory();
        assertEquals("ruleset", factory.name());
    }

    @Test
    void createShouldBuildValidatorFromRulesetPath(@TempDir Path tempDir) throws IOException {
        Path rulesetFile = tempDir.resolve("test-rules.yml");
        Files.writeString(rulesetFile, """
                rules:
                  test-rule:
                    message: "Test"
                    given: "$.info"
                    then:
                      function: truthy
                """);

        RulesetValidatorFactory factory = new RulesetValidatorFactory();
        ValidatorConfig config = new ValidatorConfig(Map.of("rulesetPath", rulesetFile.toString()));
        Validator validator = factory.create(config);

        assertNotNull(validator);
        assertEquals("ruleset", validator.name());
    }

    @Test
    void createShouldBuildValidatorFromRulesetContent() {
        String content = """
                rules:
                  test-rule:
                    message: "Test"
                    given: "$.info"
                    then:
                      function: truthy
                """;

        RulesetValidatorFactory factory = new RulesetValidatorFactory();
        ValidatorConfig config = new ValidatorConfig(Map.of("rulesetContent", content));
        Validator validator = factory.create(config);

        assertNotNull(validator);
        assertEquals("ruleset", validator.name());
    }

    @Test
    void createShouldThrowWhenNoRulesetConfigured() {
        RulesetValidatorFactory factory = new RulesetValidatorFactory();
        ValidatorConfig config = new ValidatorConfig(Map.of());

        RulesetParseException ex = assertThrows(RulesetParseException.class,
                () -> factory.create(config));
        assertTrue(ex.getMessage().contains("No ruleset configured"));
    }

    @Test
    void createShouldPassIncludeNonRecommendedOption() {
        String content = """
                rules:
                  optional-rule:
                    message: "Optional"
                    recommended: false
                    given: "$.info.name"
                    then:
                      function: truthy
                """;

        RulesetValidatorFactory factory = new RulesetValidatorFactory();
        ValidatorConfig config = new ValidatorConfig(
                Map.of("rulesetContent", content, "includeNonRecommended", true));
        Validator validator = factory.create(config);

        var doc = io.polychro.spi.Document.fromString("{\"info\": {\"name\": \"\"}}", "json");
        var diagnostics = validator.validate(doc);
        assertEquals(1, diagnostics.size());
    }

    @Test
    void supportedFormatsShouldAdvertiseProjectedFormats() {
        RulesetValidatorFactory factory = new RulesetValidatorFactory();

        assertEquals(Set.of("json", "yaml", "xml", "markdown", "html"),
                factory.supportedFormats());
    }

    @Test
    void factoryShouldBeDiscoverableViaServiceLoader() {
        ServiceLoader<ValidatorFactory> loader = ServiceLoader.load(ValidatorFactory.class);
        boolean found = false;
        for (ValidatorFactory factory : loader) {
            if ("ruleset".equals(factory.name())) {
                found = true;
                break;
            }
        }
        assertTrue(found, "RulesetValidatorFactory should be discoverable via ServiceLoader");
    }
}
