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

import io.polychro.ruleset.Ruleset;
import io.polychro.ruleset.RulesetValidatorFactory;
import io.polychro.spi.Diagnostic;
import io.polychro.spi.Document;
import io.polychro.spi.SourceRange;
import io.polychro.spi.Validator;
import io.polychro.spi.ValidatorConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the bundled {@code polychro:openapi} ruleset (naftiko/polychro#83), which ports a
 * subset of Spectral's {@code spectral:oas} to exercise:
 * <ul>
 *     <li>the JSONPath key-selector operator ({@code ~}) — {@code openapi-paths-kebab-case},
 *     which targets the keys of {@code $.paths} (path strings), not their values;</li>
 *     <li>spec-level format detection/filtering ({@code oas2}/{@code oas3}) — {@code oas2-api-host}
 *     / {@code oas2-api-schemes} fire only for a {@code swagger: "2.0"} document,
 *     {@code oas3-api-servers} only for an {@code openapi: 3.x} document.</li>
 * </ul>
 *
 * <p>The exact diagnostics produced here were cross-checked against the real Spectral CLI (via
 * this module's fixtures and ruleset ported verbatim from {@code spectral:oas}) as part of
 * developing this ruleset; see the Phase 0 conformance harness ({@code polychro-conformance}) for
 * the automated golden-file equivalent.
 */
class OpenApiRulesetTest {

    private static Validator validator;
    private static final Path FIXTURES = Path.of("src/test/resources/fixtures").toAbsolutePath();

    @BeforeAll
    static void setUp() {
        Ruleset ruleset = RulesetCatalog.loadAsRuleset("openapi");
        validator = new RulesetValidatorFactory().create(
                new ValidatorConfig(Map.of("ruleset", ruleset)));
    }

    @Test
    void cleanOas3CapabilityShouldPassWithNoDiagnostics() {
        Document doc = Document.fromYaml(FIXTURES.resolve("openapi-oas3-clean.yml"));
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.isEmpty(), () -> "Expected no violations but got: " + results);
    }

    @Test
    void cleanOas2CapabilityShouldPassWithNoDiagnostics() {
        Document doc = Document.fromYaml(FIXTURES.resolve("openapi-oas2-clean.yml"));
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.isEmpty(), () -> "Expected no violations but got: " + results);
    }

    @Test
    void oas3ViolationsShouldTriggerInfoContact() {
        Document doc = Document.fromYaml(FIXTURES.resolve("openapi-oas3-violations.yml"));
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.stream().anyMatch(d -> d.code().equals("info-contact")),
                () -> "Expected info-contact violation, got: " + results);
    }

    @Test
    void oas3ViolationsShouldTriggerInfoDescription() {
        Document doc = Document.fromYaml(FIXTURES.resolve("openapi-oas3-violations.yml"));
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.stream().anyMatch(d -> d.code().equals("info-description")),
                () -> "Expected info-description violation, got: " + results);
    }

    @Test
    void infoContactShouldFireWhenInfoObjectIsEntirelyAbsent() {
        // Spectral defines info-contact with given: "$" and field: "info.contact", so it still
        // reports the missing required value even when the whole "info" object is absent —
        // a narrower given: "$.info" selector would produce no match at all and silently
        // suppress the rule instead.
        String yaml = "openapi: 3.0.2\npaths: {}\n";
        Document doc = Document.fromString(yaml, "yaml");
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.stream().anyMatch(d -> d.code().equals("info-contact")
                        && d.path().equals("$")),
                () -> "Expected info-contact to fire at $ when info is entirely absent, got: " + results);
    }

    @Test
    void infoDescriptionShouldFireWhenInfoObjectIsEntirelyAbsent() {
        String yaml = "openapi: 3.0.2\npaths: {}\n";
        Document doc = Document.fromString(yaml, "yaml");
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.stream().anyMatch(d -> d.code().equals("info-description")
                        && d.path().equals("$")),
                () -> "Expected info-description to fire at $ when info is entirely absent, got: " + results);
    }

    @Test
    void licenseUrlShouldFireWhenInfoOrLicenseIsAbsent() {
        // The non-recommended license-url rule must still report the missing nested value when
        // info.license is absent entirely, per Spectral's given: "$" + field: "info.license.url".
        Ruleset ruleset = RulesetCatalog.loadAsRuleset("openapi");
        Validator nonRecommendedValidator = new RulesetValidatorFactory().create(
                new ValidatorConfig(Map.of(
                        "ruleset", ruleset,
                        "includeNonRecommended", true)));
        String yaml = "openapi: 3.0.2\ninfo:\n  title: Test\n  version: \"1.0\"\npaths: {}\n";
        Document doc = Document.fromString(yaml, "yaml");
        List<Diagnostic> results = nonRecommendedValidator.validate(doc);
        assertTrue(results.stream().anyMatch(d -> d.code().equals("license-url")
                        && d.path().equals("$.info")),
                () -> "Expected license-url to fire at $.info when license is absent, got: " + results);
    }

    @Test
    void oas3ViolationsShouldTriggerKeySelectorKebabCaseRuleOnPathKey() throws Exception {
        // Issue #83: the `~` key selector must match the /Pets path KEY, not a value under it —
        // exercised here against $.paths.*~. Loaded through Document.fromString (not fromYaml)
        // so a real JacksonSourceMap is built and the range can be asserted, not SourceMap.NONE.
        Path fixture = FIXTURES.resolve("openapi-oas3-violations.yml");
        String content = java.nio.file.Files.readString(fixture);
        Document doc = Document.fromString(content, "yaml", fixture.toString());
        List<Diagnostic> results = validator.validate(doc);
        List<Diagnostic> kebabViolations = results.stream()
                .filter(d -> d.code().equals("openapi-paths-kebab-case"))
                .toList();
        assertEquals(1, kebabViolations.size(),
                () -> "Expected exactly one kebab-case violation, got: " + results);
        assertEquals("$.paths./Pets", kebabViolations.get(0).path());

        // A key-selector diagnostic must report on the KEY's own source location, not the
        // value node it is keyed to — SourceMap.resolveKey() (built on JacksonSourceMap's
        // field-name tracking) resolves this distinctly from resolve(). The fixture's "/Pets"
        // key sits on line 9 (0-based 8), indented 2 spaces, and the quote-less key spans
        // columns 2..7 ("/Pets", 5 chars).
        SourceRange range = kebabViolations.get(0).range();
        assertNotNull(range,
                "the ~ key-selector diagnostic must carry a SourceRange, not null "
                        + "(requires loading the fixture through fromString, not fromYaml/SourceMap.NONE)");
        assertEquals(8, range.startLine(), "range must point at the /Pets KEY (line 9), not its value");
        assertEquals(2, range.startColumn());
        assertEquals(8, range.endLine());
        assertEquals(7, range.endColumn());
    }

    @Test
    void oas3ViolationsShouldTriggerOas3ApiServersButNotOas2Rules() {
        // Spec-level format filtering (issue #83): an openapi: 3.x document must fire the
        // oas3-scoped rule and never the oas2-scoped ones.
        Document doc = Document.fromYaml(FIXTURES.resolve("openapi-oas3-violations.yml"));
        List<Diagnostic> results = validator.validate(doc);
        Set<String> codes = results.stream().map(Diagnostic::code).collect(java.util.stream.Collectors.toSet());
        assertTrue(codes.contains("oas3-api-servers"), () -> "Expected oas3-api-servers, got: " + results);
        assertTrue(codes.stream().noneMatch(c -> c.startsWith("oas2-")),
                () -> "oas2-scoped rules must not fire on an OpenAPI v3 document, got: " + results);
    }

    @Test
    void oas2ViolationsShouldTriggerOas2RulesButNotOas3Rules() {
        // Spec-level format filtering (issue #83): a swagger: "2.0" document must fire the
        // oas2-scoped rules and never the oas3-scoped one.
        Document doc = Document.fromYaml(FIXTURES.resolve("openapi-oas2-violations.yml"));
        List<Diagnostic> results = validator.validate(doc);
        Set<String> codes = results.stream().map(Diagnostic::code).collect(java.util.stream.Collectors.toSet());
        assertTrue(codes.contains("oas2-api-host"), () -> "Expected oas2-api-host, got: " + results);
        assertTrue(codes.contains("oas2-api-schemes"), () -> "Expected oas2-api-schemes, got: " + results);
        assertTrue(codes.stream().noneMatch(c -> c.startsWith("oas3-")),
                () -> "oas3-scoped rules must not fire on a Swagger v2 document, got: " + results);
    }

    @Test
    void oas2ViolationsShouldTriggerKeySelectorKebabCaseRuleOnPathKey() {
        Document doc = Document.fromYaml(FIXTURES.resolve("openapi-oas2-violations.yml"));
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.stream().anyMatch(d -> d.code().equals("openapi-paths-kebab-case")
                        && d.path().equals("$.paths./Pets")),
                () -> "Expected openapi-paths-kebab-case on $.paths./Pets, got: " + results);
    }

    @Test
    void specificationExtensionPathKeysWithNonAlphanumericCharsShouldNotTriggerKebabCaseRule() {
        // OpenAPI only requires a Paths Object specification-extension key to start with `x-` —
        // it imposes no further character restriction. `x-internal_note` and `x-acme.foo` are
        // both valid extension names and must not trigger openapi-paths-kebab-case, even though
        // `_` and `.` fall outside [a-zA-Z0-9-].
        String yaml = "openapi: 3.0.2\n"
                + "info:\n"
                + "  title: Test API\n"
                + "  version: \"1.0\"\n"
                + "paths:\n"
                + "  /pets:\n"
                + "    get:\n"
                + "      responses:\n"
                + "        \"200\":\n"
                + "          description: ok\n"
                + "  x-internal_note: internal use only\n"
                + "  x-acme.foo: vendor extension\n";
        Document doc = Document.fromString(yaml, "yaml");
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.stream().noneMatch(d -> d.code().equals("openapi-paths-kebab-case")),
                () -> "x-* extension keys must never trigger openapi-paths-kebab-case, got: " + results);
    }

    @Test
    void pathParameterNamesWithHyphensShouldNotTriggerKebabCaseRule() {
        // OpenAPI path-parameter names are not restricted to [a-zA-Z0-9_] — a name like
        // "pet-id" is valid, so the placeholder branch of openapi-paths-kebab-case's pattern
        // must accept hyphens inside "{...}" too, and casing validation must stay limited to
        // literal path segments.
        String yaml = "openapi: 3.0.2\n"
                + "info:\n"
                + "  title: Test API\n"
                + "  version: \"1.0\"\n"
                + "paths:\n"
                + "  /pets/{pet-id}:\n"
                + "    get:\n"
                + "      parameters:\n"
                + "        - name: pet-id\n"
                + "          in: path\n"
                + "          required: true\n"
                + "          schema:\n"
                + "            type: string\n"
                + "      responses:\n"
                + "        \"200\":\n"
                + "          description: ok\n";
        Document doc = Document.fromString(yaml, "yaml");
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.stream().noneMatch(d -> d.code().equals("openapi-paths-kebab-case")),
                () -> "a hyphenated path-parameter name must never trigger openapi-paths-kebab-case, got: "
                        + results);
    }

    // --- Positive-violation coverage for rules with no prior fixture (comprehensive coverage
    // policy): license-url, no-eval-in-markdown, no-script-tags-in-markdown, openapi-tags, and
    // oas3-server-trailing-slash previously had no fixture that made them actually report a
    // violation, so a miswired `given`/`then`/`functionOptions` would have gone undetected. ---

    @Test
    void additionalViolationsShouldTriggerNoEvalInMarkdown() {
        Document doc = Document.fromYaml(FIXTURES.resolve("openapi-oas3-additional-violations.yml"));
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.stream().anyMatch(d -> d.code().equals("no-eval-in-markdown")),
                () -> "Expected no-eval-in-markdown violation, got: " + results);
    }

    @Test
    void additionalViolationsShouldTriggerNoScriptTagsInMarkdown() {
        Document doc = Document.fromYaml(FIXTURES.resolve("openapi-oas3-additional-violations.yml"));
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.stream().anyMatch(d -> d.code().equals("no-script-tags-in-markdown")),
                () -> "Expected no-script-tags-in-markdown violation, got: " + results);
    }

    @Test
    void additionalViolationsShouldTriggerOas3ServerTrailingSlash() {
        Document doc = Document.fromYaml(FIXTURES.resolve("openapi-oas3-additional-violations.yml"));
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.stream().anyMatch(d -> d.code().equals("oas3-server-trailing-slash")),
                () -> "Expected oas3-server-trailing-slash violation, got: " + results);
    }

    @Test
    void rootRelativeServerUrlShouldNotTriggerOas3ServerTrailingSlash() {
        // Spectral's real notMatch pattern is "./$" (any character followed by a slash at the
        // end), not "/$" — so the root-relative server URL "/" is a valid, distinct value that
        // must NOT be flagged, even though it "ends with a slash" under a naive reading.
        String yaml = "openapi: 3.0.2\n"
                + "info:\n"
                + "  title: Test API\n"
                + "  version: \"1.0\"\n"
                + "servers:\n"
                + "  - url: /\n"
                + "paths:\n"
                + "  /pets:\n"
                + "    get:\n"
                + "      responses:\n"
                + "        \"200\":\n"
                + "          description: ok\n";
        Document doc = Document.fromString(yaml, "yaml");
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.stream().noneMatch(d -> d.code().equals("oas3-server-trailing-slash")),
                () -> "the root-relative server URL \"/\" must never trigger oas3-server-trailing-slash, got: "
                        + results);
    }

    @Test
    void additionalViolationsShouldTriggerLicenseUrlAndOpenapiTagsWhenNonRecommendedRulesAreIncluded() {
        // license-url and openapi-tags are both `recommended: false`, so they are silent under
        // the default validator (built with includeNonRecommended unset/false, per this test
        // class's setUp()) — a separate validator instance is required to exercise them.
        Ruleset ruleset = RulesetCatalog.loadAsRuleset("openapi");
        Validator nonRecommendedValidator = new RulesetValidatorFactory().create(
                new ValidatorConfig(Map.of(
                        "ruleset", ruleset,
                        "includeNonRecommended", true)));
        Document doc = Document.fromYaml(FIXTURES.resolve("openapi-oas3-additional-violations.yml"));
        List<Diagnostic> results = nonRecommendedValidator.validate(doc);
        assertTrue(results.stream().anyMatch(d -> d.code().equals("license-url")),
                () -> "Expected license-url violation, got: " + results);
        assertTrue(results.stream().anyMatch(d -> d.code().equals("openapi-tags")),
                () -> "Expected openapi-tags violation, got: " + results);
    }
}
