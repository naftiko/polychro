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

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OverrideResolverTest {

    private final OverrideResolver resolver = new OverrideResolver();

    @Test
    void overrideRecordShouldHandleNullFields() {
        RulesetOverride override = new RulesetOverride(null, null, null, null);
        assertEquals(List.of(), override.files());
        assertEquals(Map.of(), override.rules());
        assertEquals(Map.of(), override.aliases());
        assertNull(override.formats());
    }

    @Test
    void nullDocumentPathShouldNotMatchAnyOverride() {
        Rule rule = new Rule("test", "msg", null, "warn", true, null, null,
                List.of("$"), List.of(new RuleAction("truthy", null, null)));
        RulesetOverride override = new RulesetOverride(
                List.of("schemas/**"), Map.of("test", rule), Map.of(), null);

        Map<String, Rule> result = resolver.applyOverrides(
                Map.of(), List.of(override), null);

        assertTrue(result.isEmpty());
    }

    @Test
    void emptyFilePatternsShouldNotMatch() {
        assertFalse(resolver.matchesDocument(List.of(), "some/path.json"));
    }

    @Test
    void nullFilePatternsShouldNotMatch() {
        assertFalse(resolver.matchesDocument(null, "some/path.json"));
    }

    @Test
    void emptyGlobShouldMatchAll() {
        // File pattern with only a JSON Pointer (empty glob part)
        assertTrue(resolver.matchesDocument(List.of("#/definitions"), "any/file.json"));
    }

    @Test
    void questionMarkGlobShouldMatchSingleChar() {
        assertTrue(resolver.matchesDocument(List.of("schema?.json"), "schema1.json"));
        assertFalse(resolver.matchesDocument(List.of("schema?.json"), "schema12.json"));
    }

    @Test
    void extractJsonPointerShouldReturnPointerAfterHash() {
        assertEquals("/definitions/Pet", resolver.extractJsonPointer("schemas/**#/definitions/Pet"));
    }

    @Test
    void extractJsonPointerShouldReturnNullWhenNoPointer() {
        assertNull(resolver.extractJsonPointer("schemas/**/*.json"));
    }

    @Test
    void extractJsonPointerShouldReturnNullForNonPathHash() {
        // Hash at end with no slash after it
        assertNull(resolver.extractJsonPointer("schemas#notapointer"));
    }

    @Test
    void extractGlobShouldReturnPartBeforeHash() {
        assertEquals("schemas/**", resolver.extractGlob("schemas/**#/definitions"));
    }

    @Test
    void extractGlobShouldReturnFullPatternWhenNoHash() {
        assertEquals("schemas/**/*.json", resolver.extractGlob("schemas/**/*.json"));
    }

    @Test
    void collectOverrideAliasesShouldReturnEmptyForNullOverrides() {
        assertEquals(Map.of(), resolver.collectOverrideAliases(null, "file.json"));
    }

    @Test
    void getOverrideFormatsShouldReturnNullForNullOverrides() {
        assertNull(resolver.getOverrideFormats(null, "file.json"));
    }

    @Test
    void getOverrideFormatsShouldReturnNullWhenNoFormatsMatch() {
        RulesetOverride override = new RulesetOverride(
                List.of("schemas/**"), Map.of(), Map.of(), null);
        assertNull(resolver.getOverrideFormats(List.of(override), "schemas/model.json"));
    }

    @Test
    void getOverrideFormatsShouldReturnFormatsWhenMatchingDocument() {
        RulesetOverride override = new RulesetOverride(
                List.of("schemas/**"), Map.of(), Map.of(), List.of("oas3"));
        assertEquals(List.of("oas3"),
                resolver.getOverrideFormats(List.of(override), "schemas/model.json"));
    }

    @Test
    void doubleStarAtEndWithoutSlashShouldMatchAnything() {
        // ** without trailing / should match any remaining path
        assertTrue(resolver.matchesDocument(List.of("src/**"), "src/main/file.java"));
    }

    @Test
    void emptyOverridesListShouldReturnBaseRules() {
        Rule rule = new Rule("test", "msg", null, "warn", true, null, null,
                List.of("$"), List.of(new RuleAction(null, "truthy", Map.of())));
        Map<String, Rule> result = resolver.applyOverrides(Map.of("test", rule), List.of(), "any.json");
        assertEquals(1, result.size());
    }

    @Test
    void nullGlobPartShouldMatchAll() {
        // extractGlob returns empty string for "#/pointer" pattern → matchesGlob("", path) = true
        assertTrue(resolver.matchesDocument(List.of("#/definitions"), "any/file.json"));
    }

    @Test
    void singleStarAtEndShouldMatchOneSegment() {
        assertTrue(resolver.matchesDocument(List.of("src/*"), "src/file.txt"));
        assertFalse(resolver.matchesDocument(List.of("src/*"), "src/sub/file.txt"));
    }

    @Test
    void doubleStarSlashPatternShouldMatchNestedPaths() {
        assertTrue(resolver.matchesDocument(List.of("**/test.json"), "a/b/test.json"));
        assertTrue(resolver.matchesDocument(List.of("**/test.json"), "test.json"));
    }

    @Test
    void doubleStarFollowedByNonSlashShouldMatchAnything() {
        // **x pattern — ** not followed by /
        assertTrue(resolver.matchesDocument(List.of("**.json"), "src/file.json"));
    }

    @Test
    void applyOverridesWithNullListShouldReturnBaseRules() {
        Rule rule = new Rule("test", "msg", null, "warn", true, null, null,
                List.of("$"), List.of(new RuleAction(null, "truthy", Map.of())));
        Map<String, Rule> result = resolver.applyOverrides(Map.of("test", rule), null, "any.json");
        assertEquals(1, result.size());
    }
}
