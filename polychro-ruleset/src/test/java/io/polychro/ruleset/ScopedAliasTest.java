package io.polychro.ruleset;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ScopedAliasTest {

    private final RulesetParser parser = new RulesetParser();

    @Test
    void scopedAliasWithTargetsShouldParseFirstTargetGiven() {
        String yaml = """
                aliases:
                  PathItem:
                    targets:
                      - formats:
                          - oas2
                        given: "$.paths[*]"
                      - formats:
                          - oas3
                        given: "$.paths[*]"
                rules: {}
                """;

        Ruleset ruleset = parser.parse(yaml);

        // First target's given is used as the alias value
        assertEquals("$.paths[*]", ruleset.aliases().get("PathItem"));
    }

    @Test
    void scopedAliasWithArrayGivenShouldUseFirstElement() {
        String yaml = """
                aliases:
                  Parameters:
                    targets:
                      - formats:
                          - oas2
                        given:
                          - "$.parameters[*]"
                          - "$.paths[*].parameters[*]"
                rules: {}
                """;

        Ruleset ruleset = parser.parse(yaml);

        assertEquals("$.parameters[*]", ruleset.aliases().get("Parameters"));
    }

    @Test
    void multipleScopedAliasesShouldAllParse() {
        String yaml = """
                aliases:
                  PathItem:
                    targets:
                      - formats:
                          - oas3
                        given: "$.paths[*]"
                  Operation:
                    targets:
                      - formats:
                          - oas3
                        given: "$.paths[*][get,put,post,delete]"
                rules: {}
                """;

        Ruleset ruleset = parser.parse(yaml);

        assertEquals("$.paths[*]", ruleset.aliases().get("PathItem"));
        assertEquals("$.paths[*][get,put,post,delete]", ruleset.aliases().get("Operation"));
    }

    @Test
    void mixedSimpleAndScopedAliasesShouldCoexist() {
        String yaml = """
                aliases:
                  Info: "$.info"
                  PathItem:
                    targets:
                      - formats:
                          - oas3
                        given: "$.paths[*]"
                rules: {}
                """;

        Ruleset ruleset = parser.parse(yaml);

        assertEquals("$.info", ruleset.aliases().get("Info"));
        assertEquals("$.paths[*]", ruleset.aliases().get("PathItem"));
    }

    @Test
    void aliasWithEmptyTargetsShouldNotBeAdded() {
        String yaml = """
                aliases:
                  Empty:
                    targets: []
                rules: {}
                """;

        Ruleset ruleset = parser.parse(yaml);

        assertFalse(ruleset.aliases().containsKey("Empty"));
    }
}
