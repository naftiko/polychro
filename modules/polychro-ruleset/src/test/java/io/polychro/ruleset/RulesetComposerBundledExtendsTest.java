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

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers bundled {@code extends} reference resolution and tuple-form severity remapping
 * (naftiko/polychro#84 — Phase 3 of the Spectral iso-functionality track, naftiko/polychro#76).
 *
 * <p>Fixtures reference the real bundled {@code polychro:openapi} ruleset (which stands in for
 * {@code spectral:oas}) rather than a synthetic ruleset, so a change to that ruleset's rule
 * count or {@code recommended} flags is caught here too.
 */
class RulesetComposerBundledExtendsTest {

    private final RulesetParser parser = new RulesetParser();
    private final RulesetComposer composer = new RulesetComposer(parser);

    @Test
    void bareSpectralOasRefShouldResolveToBundledOpenapiRuleset() {
        Ruleset child = new Ruleset(java.util.List.of("spectral:oas"), Map.of(), java.util.List.of(),
                java.util.List.of(), java.util.List.of(), Map.of(), null);

        Ruleset composed = composer.compose(child);

        assertTrue(composed.rules().containsKey("info-contact"));
        assertTrue(composed.rules().containsKey("openapi-paths-kebab-case"));
    }

    @Test
    void directPolychroRefShouldAlsoResolve() {
        Ruleset child = new Ruleset(java.util.List.of("polychro:openapi"), Map.of(), java.util.List.of(),
                java.util.List.of(), java.util.List.of(), Map.of(), null);

        Ruleset composed = composer.compose(child);

        assertTrue(composed.rules().containsKey("info-contact"));
    }

    @Test
    void unknownSpectralBundleShouldThrowWithKnownIdsListed() {
        Ruleset child = new Ruleset(java.util.List.of("spectral:asyncapi"), Map.of(), java.util.List.of(),
                java.util.List.of(), java.util.List.of(), Map.of(), null);

        RulesetParseException exception = assertThrows(RulesetParseException.class,
                () -> composer.compose(child));

        assertTrue(exception.getMessage().contains("spectral:asyncapi"));
        assertTrue(exception.getMessage().contains("spectral:oas"));
    }

    @Test
    void offSeverityShouldDisableEveryInheritedRule() {
        Ruleset child = new Ruleset(java.util.List.of("spectral:oas"), Map.of(), java.util.List.of(),
                java.util.List.of(), java.util.List.of(), Map.of(), null,
                Map.of("spectral:oas", "off"));

        Ruleset composed = composer.compose(child);

        assertTrue(composed.rules().values().stream().allMatch(rule -> "off".equalsIgnoreCase(rule.severity())));
    }

    @Test
    void recommendedSeverityShouldDisableOnlyNonRecommendedRules() {
        Ruleset child = new Ruleset(java.util.List.of("spectral:oas"), Map.of(), java.util.List.of(),
                java.util.List.of(), java.util.List.of(), Map.of(), null,
                Map.of("spectral:oas", "recommended"));

        Ruleset composed = composer.compose(child);

        // info-license is recommended: false in the bundled openapi ruleset — must be silenced.
        assertEquals("off", composed.rules().get("info-license").severity());
        // info-contact is recommended: true — its own severity (default "warn") passes through.
        assertTrue(!"off".equalsIgnoreCase(composed.rules().get("info-contact").severity()));
    }

    @Test
    void allSeverityShouldForceEnableEveryRuleIncludingNonRecommended() {
        Ruleset child = new Ruleset(java.util.List.of("spectral:oas"), Map.of(), java.util.List.of(),
                java.util.List.of(), java.util.List.of(), Map.of(), null,
                Map.of("spectral:oas", "all"));

        Ruleset composed = composer.compose(child);

        assertTrue(composed.rules().values().stream()
                .noneMatch(rule -> "off".equalsIgnoreCase(rule.severity())));
        assertTrue(composed.rules().values().stream().allMatch(Rule::recommended));
    }

    @Test
    void childOwnRuleShouldStillOverrideSeverityRemappedInheritedRule() {
        Rule childOverride = new Rule("info-contact", "Child message", null, "error", true,
                null, null, java.util.List.of("$.info.name"),
                java.util.List.of(new RuleAction(null, "truthy", Map.of())));
        Ruleset child = new Ruleset(java.util.List.of("spectral:oas"), Map.of(), java.util.List.of(),
                java.util.List.of(), java.util.List.of(), Map.of("info-contact", childOverride), null,
                Map.of("spectral:oas", "off"));

        Ruleset composed = composer.compose(child);

        // "off" from extends applies to every inherited rule, but the child re-declares
        // info-contact directly — the child's own rules: block wins, as it does for any
        // ordinary (non-bundled) extends composition.
        assertEquals("error", composed.rules().get("info-contact").severity());
        assertEquals("Child message", composed.rules().get("info-contact").message());
    }

    @Test
    void bareStringSpectralRefWithNoTupleShouldNotRemapSeverity() {
        // No extendsSeverities entry for "spectral:oas" — today's unchanged behavior.
        Ruleset child = new Ruleset(java.util.List.of("spectral:oas"), Map.of(), java.util.List.of(),
                java.util.List.of(), java.util.List.of(), Map.of(), null);

        Ruleset composed = composer.compose(child);

        assertTrue(composed.rules().values().stream().anyMatch(Rule::recommended));
        // info-license (recommended:false) keeps its own declared severity — not force-disabled.
        assertTrue(!"off".equalsIgnoreCase(composed.rules().get("info-license").severity()));
    }

    @Test
    void unresolvableBundledClasspathResourceShouldThrowRulesetParseException() {
        // "polychro:" refs bypass BUNDLED_ALIASES and use the name after the prefix directly, so
        // an unknown name resolves to a classpath resource that genuinely does not exist —
        // exercising the UncheckedIOException -> RulesetParseException wrapping in loadBundledRef.
        Ruleset child = new Ruleset(java.util.List.of("polychro:does-not-exist"), Map.of(), java.util.List.of(),
                java.util.List.of(), java.util.List.of(), Map.of(), null);

        RulesetParseException exception = assertThrows(RulesetParseException.class,
                () -> composer.compose(child));

        assertTrue(exception.getMessage().contains("polychro:does-not-exist"));
    }

    @Test
    void unrecognizedSeverityMarkerShouldBeANoOp() {
        // A defensive no-op: RulesetParser.parseExtendsSeverities only ever stores the literal
        // tuple string, so this can't occur from real YAML today, but applyExtendsSeverity must
        // not fail loading over an unexpected marker.
        Ruleset child = new Ruleset(java.util.List.of("spectral:oas"), Map.of(), java.util.List.of(),
                java.util.List.of(), java.util.List.of(), Map.of(), null,
                Map.of("spectral:oas", "bogus-marker"));

        Ruleset composed = composer.compose(child);

        // Rules pass through unchanged — same as the no-tuple case.
        assertTrue(!"off".equalsIgnoreCase(composed.rules().get("info-license").severity()));
    }
}
