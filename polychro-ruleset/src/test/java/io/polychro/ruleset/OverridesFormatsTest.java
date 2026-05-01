package io.polychro.ruleset;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OverridesFormatsTest {

    private final RulesetParser parser = new RulesetParser();
    private final OverrideResolver resolver = new OverrideResolver();

    @Test
    void overrideWithFormatsShouldParseCorrectly() {
        String yaml = """
                extends: []
                rules:
                  base-rule:
                    message: "Base"
                    severity: warn
                    recommended: true
                    given: "$"
                    then:
                      function: truthy
                overrides:
                  - files:
                      - "**"
                    formats:
                      - oas3
                    rules:
                      oas3-rule:
                        message: "OAS3 only"
                        severity: error
                        recommended: true
                        given: "$.openapi"
                        then:
                          function: truthy
                """;

        Ruleset ruleset = parser.parse(yaml);

        assertEquals(1, ruleset.overrides().size());
        assertEquals(List.of("oas3"), ruleset.overrides().get(0).formats());
    }

    @Test
    void getOverrideFormatsShouldReturnFormatsForMatchingDocument() {
        RulesetOverride override = new RulesetOverride(
                List.of("**"), Map.of(), Map.of(), List.of("oas3"));

        List<String> formats = resolver.getOverrideFormats(List.of(override), "api/spec.yaml");

        assertEquals(List.of("oas3"), formats);
    }

    @Test
    void getOverrideFormatsShouldReturnNullWhenNoFormats() {
        RulesetOverride override = new RulesetOverride(
                List.of("**"), Map.of(), Map.of(), null);

        List<String> formats = resolver.getOverrideFormats(List.of(override), "api/spec.yaml");

        assertNull(formats);
    }

    @Test
    void getOverrideFormatsShouldReturnNullForNonMatchingDocument() {
        RulesetOverride override = new RulesetOverride(
                List.of("schemas/**"), Map.of(), Map.of(), List.of("oas3"));

        List<String> formats = resolver.getOverrideFormats(List.of(override), "api/spec.yaml");

        assertNull(formats);
    }

    @Test
    void getOverrideFormatsShouldReturnNullForEmptyOverrides() {
        List<String> formats = resolver.getOverrideFormats(List.of(), "api/spec.yaml");
        assertNull(formats);
    }

    @Test
    void getOverrideFormatsShouldReturnNullForNullOverrides() {
        List<String> formats = resolver.getOverrideFormats(null, "api/spec.yaml");
        assertNull(formats);
    }
}
