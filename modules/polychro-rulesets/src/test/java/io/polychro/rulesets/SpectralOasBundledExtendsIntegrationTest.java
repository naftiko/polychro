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

import io.polychro.spi.Diagnostic;
import io.polychro.spi.Document;
import io.polychro.spi.Validator;
import io.polychro.spi.ValidatorConfig;
import io.polychro.ruleset.RulesetValidatorFactory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end proof that {@code extends: spectral:oas} resolves against the REAL bundled
 * {@code polychro:openapi} ruleset shipped in this module's
 * {@code src/main/resources/rulesets/openapi.yml} — not a synthetic stand-in.
 *
 * <p>{@code polychro-ruleset}'s own {@code RulesetComposerBundledExtendsTest} covers the
 * resolution MECHANISM (constants, cycle detection, severity remap) against a lightweight
 * test-only stub, because that module has no visibility of this one's resources (one-way
 * dependency: this module depends on {@code polychro-ruleset}, not the reverse). This class
 * closes that gap by driving the public {@link RulesetValidatorFactory} API from a module that
 * can see both the composer and the real shipped resource, exactly as a real consumer capability
 * YAML would (naftiko/polychro#84).
 */
class SpectralOasBundledExtendsIntegrationTest {

    @Test
    void extendsSpectralOasShouldResolveToRealBundledOpenapiRuleset() {
        String childRuleset = """
                extends: "spectral:oas"
                rules: {}
                """;
        Validator validator = new RulesetValidatorFactory().create(
                new ValidatorConfig(Map.of("rulesetContent", childRuleset)));

        // Empty info object — should fire the real openapi.yml's info-contact rule (recommended).
        // "openapi: 3.0.2" is required for the document to match the ruleset-level formats:
        // [oas2, oas3] gate (RulesetValidator.matchesFormat) that info-contact relies on.
        Document doc = Document.fromString("{\"openapi\": \"3.0.2\", \"info\": {}}", "json");
        List<Diagnostic> results = validator.validate(doc);

        assertTrue(results.stream().anyMatch(d -> d.code().equals("info-contact")));
    }

    @Test
    void extendsSpectralOasOffShouldSilenceEveryRealBundledRule() {
        String childRuleset = """
                extends:
                  - ["spectral:oas", "off"]
                rules: {}
                """;
        Validator validator = new RulesetValidatorFactory().create(
                new ValidatorConfig(Map.of("rulesetContent", childRuleset)));

        Document doc = Document.fromString("{\"openapi\": \"3.0.2\", \"info\": {}}", "json");
        List<Diagnostic> results = validator.validate(doc);

        assertTrue(results.isEmpty(), "Expected no diagnostics but got: " + results);
    }

    @Test
    void extendsSpectralOasAllShouldEnableNonRecommendedRealBundledRules() {
        String childRuleset = """
                extends:
                  - ["spectral:oas", "all"]
                rules: {}
                """;
        Validator validator = new RulesetValidatorFactory().create(
                new ValidatorConfig(Map.of("rulesetContent", childRuleset)));

        // info-license is recommended:false in the real bundled ruleset — "all" must enable it.
        Document doc = Document.fromString("{\"openapi\": \"3.0.2\", \"info\": {}}", "json");
        List<Diagnostic> results = validator.validate(doc);

        assertTrue(results.stream().anyMatch(d -> d.code().equals("info-license")));
    }

    @Test
    void extendsUnknownSpectralBundleShouldThrowNamingKnownBundles() {
        String childRuleset = """
                extends: "spectral:asyncapi"
                rules: {}
                """;

        RuntimeException exception = org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class,
                () -> new RulesetValidatorFactory().create(
                        new ValidatorConfig(Map.of("rulesetContent", childRuleset))));

        assertTrue(exception.getMessage().contains("spectral:oas"));
    }
}
