package io.polychro.ruleset;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OverridesJsonPointerTest {

    private final OverrideResolver resolver = new OverrideResolver();

    @Test
    void shouldExtractJsonPointerFromFilePattern() {
        String pointer = resolver.extractJsonPointer("**#/paths");
        assertEquals("/paths", pointer);
    }

    @Test
    void shouldExtractJsonPointerWithEncodedCharacters() {
        String pointer = resolver.extractJsonPointer("schemas/**#/definitions/~0name");
        assertEquals("/definitions/~0name", pointer);
    }

    @Test
    void shouldExtractJsonPointerWithSlashEncoding() {
        String pointer = resolver.extractJsonPointer("**#/paths/~1users/get");
        assertEquals("/paths/~1users/get", pointer);
    }

    @Test
    void shouldReturnNullWhenNoPointerPresent() {
        String pointer = resolver.extractJsonPointer("schemas/**/*.json");
        assertNull(pointer);
    }

    @Test
    void shouldReturnNullWhenHashIsAtEnd() {
        String pointer = resolver.extractJsonPointer("schemas/**#");
        assertNull(pointer);
    }

    @Test
    void shouldReturnNullWhenHashNotFollowedBySlash() {
        String pointer = resolver.extractJsonPointer("file#anchor");
        assertNull(pointer);
    }

    @Test
    void shouldExtractGlobBeforePointer() {
        String glob = resolver.extractGlob("schemas/**#/definitions");
        assertEquals("schemas/**", glob);
    }

    @Test
    void shouldExtractFullPatternWhenNoPointer() {
        String glob = resolver.extractGlob("schemas/**/*.json");
        assertEquals("schemas/**/*.json", glob);
    }

    @Test
    void pointerTargetingWithGlobShouldMatch() {
        List<String> files = List.of("**#/paths");
        // The glob part is "**" — should match any path
        boolean matches = resolver.matchesDocument(files, "api/openapi.yaml");
        assertTrue(matches);
    }

    @Test
    void pointerTargetingWithSpecificGlobShouldOnlyMatchGlob() {
        List<String> files = List.of("schemas/**#/definitions/Pet");
        boolean matchesSchemas = resolver.matchesDocument(files, "schemas/pet.json");
        boolean matchesOther = resolver.matchesDocument(files, "api/spec.json");

        assertTrue(matchesSchemas);
        assertFalse(matchesOther);
    }
}
