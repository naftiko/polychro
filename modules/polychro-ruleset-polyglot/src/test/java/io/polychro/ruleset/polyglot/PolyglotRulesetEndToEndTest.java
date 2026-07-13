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
package io.polychro.ruleset.polyglot;

import io.polychro.ruleset.RulesetValidatorFactory;
import io.polychro.spi.Diagnostic;
import io.polychro.spi.Document;
import io.polychro.spi.SourceRange;
import io.polychro.spi.Validator;
import io.polychro.spi.ValidatorConfig;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end test of the polyglot custom-function path through the public
 * {@link io.polychro.spi.Validator} API (issue #32, Layer 1).
 *
 * <p>This exercises the full production wiring that the unit tests in this module
 * deliberately bypass: a Spectral-format ruleset declares {@code functionsDir} +
 * {@code functions}, and validation must (a) actually <em>execute</em> the custom
 * JavaScript function via {@link RulesetValidatorFactory}/{@code RulesetValidator},
 * and (b) attach a non-null {@link SourceRange} resolved from the per-violation
 * {@code path} the function returns.
 *
 * <p>Until the wiring (FunctionProvider SPI context + RulesetValidator delegation)
 * and the path propagation (PolyglotRuleFunction → Violation.path) are implemented,
 * this test is RED: the custom rule never fires, so {@code validate} returns zero
 * diagnostics.
 */
class PolyglotRulesetEndToEndTest {

    private static final Path FUNCTIONS_DIR =
            Path.of("src/test/resources/functions").toAbsolutePath();

    /**
     * A ruleset that wires the {@code unique-namespaces} JS function. The function is
     * applied at the document root ({@code given: $}); it returns one result object per
     * duplicate namespace, each carrying a {@code message} and a {@code path}.
     */
    private static final String RULESET = """
            functionsDir: %s
            functions:
              - unique-namespaces
            rules:
              naftiko-unique-namespaces:
                description: Namespaces must be globally unique.
                severity: error
                given: $
                then:
                  function: unique-namespaces
            """.formatted(FUNCTIONS_DIR.toString().replace("\\", "/"));

    /**
     * Two consumes entries share the namespace {@code dup}, so the custom function
     * must report a violation pointing at the second occurrence.
     */
    private static final String DOCUMENT = """
            consumes:
              - namespace: dup
                baseUri: https://a.example.com
              - namespace: dup
                baseUri: https://b.example.com
            """;

    private Validator newValidator() {
        return new RulesetValidatorFactory()
                .create(new ValidatorConfig(Map.of("rulesetContent", RULESET)));
    }

    @Test
    void customJsRuleShouldFireThroughTheValidatorApi() {
        Validator validator = newValidator();
        Document doc = Document.fromString(DOCUMENT, "yaml");

        List<Diagnostic> diagnostics = validator.validate(doc);

        assertFalse(diagnostics.isEmpty(),
                "the custom JS function 'unique-namespaces' must fire end-to-end through "
                        + "the validator API, but produced zero diagnostics (wiring missing)");
        assertTrue(diagnostics.stream()
                        .anyMatch(d -> d.code().equals("naftiko-unique-namespaces")),
                "diagnostic must carry the rule name as its code");
    }

    @Test
    void customJsDiagnosticShouldCarrySourceRange() {
        Validator validator = newValidator();
        Document doc = Document.fromString(DOCUMENT, "yaml");

        Diagnostic diagnostic = validator.validate(doc).stream()
                .filter(d -> d.code().equals("naftiko-unique-namespaces"))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "expected a 'naftiko-unique-namespaces' diagnostic from the custom JS function"));

        // The JS function returns path ["consumes", 1, "namespace"] for the SECOND (duplicate)
        // entry; extractPath normalises it to "consumes[1].namespace" and the executor combines it
        // with the root given ("$") into the concrete path below.
        assertEquals("$.consumes[1].namespace", diagnostic.path(),
                "the per-violation JS path must be combined with the rule's given path");

        // Golden, iso-Spectral (0-based, end-exclusive): the duplicate "dup" value sits on
        // 0-based line 3 ("  - namespace: dup"), with "dup" spanning columns 15..18.
        SourceRange range = diagnostic.range();
        assertNotNull(range, "the polyglot diagnostic must carry a SourceRange resolved from the "
                + "per-violation path returned by the JS function (issue #32, Layer 1)");
        assertEquals(new SourceRange(3, 15, 3, 18), range,
                "range must point at the second 'dup' value (iso-Spectral 0-based, end-exclusive)");
    }
}
