package io.polychro.ruleset;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AliasResolutionTest {

    private final AliasResolver resolver = new AliasResolver();

    @Test
    void globalAliasShouldResolveToJsonPath() {
        Map<String, String> aliases = Map.of("Info", "$.info");
        List<String> given = List.of("#Info");

        List<String> resolved = resolver.resolve(given, aliases);

        assertEquals(List.of("$.info"), resolved);
    }

    @Test
    void aliasChainingWithPropertyShouldAppendSuffix() {
        Map<String, String> aliases = Map.of("Info", "$.info");
        List<String> given = List.of("#Info.description");

        List<String> resolved = resolver.resolve(given, aliases);

        assertEquals(List.of("$.info.description"), resolved);
    }

    @Test
    void unknownAliasShouldThrowException() {
        Map<String, String> aliases = Map.of("Info", "$.info");
        List<String> given = List.of("#Unknown");

        assertThrows(RulesetParseException.class, () -> resolver.resolve(given, aliases));
    }

    @Test
    void aliasReferencingAnotherAliasShouldResolveRecursively() {
        Map<String, String> aliases = Map.of(
                "Root", "$.capability",
                "Consumes", "#Root.consumes[*]"
        );
        List<String> given = List.of("#Consumes");

        // #Consumes -> #Root.consumes[*] -> $.capability.consumes[*]
        List<String> resolved = resolver.resolve(given, aliases);

        assertEquals(List.of("$.capability.consumes[*]"), resolved);
    }

    @Test
    void emptyAliasMapShouldReturnExpressionsUnchanged() {
        List<String> given = List.of("$.info.name", "#NotAnAlias");

        // Empty aliases — expressions without alias should pass through
        List<String> resolved = resolver.resolve(given, Map.of());

        assertEquals(given, resolved);
    }

    @Test
    void nullGivenShouldReturnEmptyList() {
        List<String> resolved = resolver.resolve(null, Map.of("Info", "$.info"));
        assertEquals(List.of(), resolved);
    }

    @Test
    void emptyGivenShouldReturnEmptyList() {
        List<String> resolved = resolver.resolve(List.of(), Map.of("Info", "$.info"));
        assertEquals(List.of(), resolved);
    }

    @Test
    void expressionWithoutHashShouldPassThrough() {
        Map<String, String> aliases = Map.of("Info", "$.info");
        List<String> given = List.of("$.info.name", "$.version");

        List<String> resolved = resolver.resolve(given, aliases);

        assertEquals(List.of("$.info.name", "$.version"), resolved);
    }

    @Test
    void mixedAliasAndPlainExpressionsShouldResolve() {
        Map<String, String> aliases = Map.of("Info", "$.info", "Root", "$");
        List<String> given = List.of("#Info.name", "$.version", "#Root");

        List<String> resolved = resolver.resolve(given, aliases);

        assertEquals(List.of("$.info.name", "$.version", "$"), resolved);
    }

    @Test
    void nullAliasMapShouldReturnExpressionsUnchanged() {
        List<String> given = List.of("$.info.name");
        List<String> resolved = resolver.resolve(given, null);
        assertEquals(given, resolved);
    }

    @Test
    void invalidAliasPatternShouldReturnExpressionUnchanged() {
        // Starts with # but doesn't match the alias pattern (e.g., #123)
        Map<String, String> aliases = Map.of("Info", "$.info");
        String result = resolver.resolveExpression("#123invalid", aliases);
        assertEquals("#123invalid", result);
    }

    @Test
    void nullExpressionShouldReturnNull() {
        Map<String, String> aliases = Map.of("Info", "$.info");
        String result = resolver.resolveExpression(null, aliases);
        assertNull(result);
    }

    @Test
    void aliasSuffixShouldBeNullSafe() {
        // Alias with no suffix — suffix group(2) should be empty string, not null
        Map<String, String> aliases = Map.of("Root", "$");
        String result = resolver.resolveExpression("#Root", aliases);
        assertEquals("$", result);
    }
}
