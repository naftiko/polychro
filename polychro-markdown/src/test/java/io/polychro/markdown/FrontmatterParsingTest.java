package io.polychro.markdown;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrontmatterParsingTest {

    private final FrontmatterParser parser = new FrontmatterParser();

    @Test
    void parseShouldExtractValidFrontmatter() {
        String content = "---\nname: test\nversion: \"1.0\"\n---\n# Title\n";
        FrontmatterResult result = parser.parse(content);
        assertFalse(result.hasError());
        assertTrue(result.hasFrontmatter());
        assertEquals("test", result.data().get("name").asText());
    }

    @Test
    void parseShouldReportMissingClosingDelimiter() {
        String content = "---\nname: test\n# Title\n";
        FrontmatterResult result = parser.parse(content);
        assertTrue(result.hasError());
        assertTrue(result.errorMessage().contains("closing"));
    }

    @Test
    void parseShouldHandleEmptyFrontmatter() {
        String content = "---\n---\n# Title\n";
        FrontmatterResult result = parser.parse(content);
        assertFalse(result.hasError());
        assertFalse(result.hasFrontmatter());
    }

    @Test
    void parseShouldReportNonMappingYaml() {
        String content = "---\n- item1\n- item2\n---\n# Title\n";
        FrontmatterResult result = parser.parse(content);
        assertTrue(result.hasError());
        assertTrue(result.errorMessage().contains("mapping"));
    }

    @Test
    void parseShouldReturnNullForNoFrontmatter() {
        String content = "# Title\n\nSome text.\n";
        FrontmatterResult result = parser.parse(content);
        assertFalse(result.hasError());
        assertFalse(result.hasFrontmatter());
        assertNull(result.data());
    }

    @Test
    void parseShouldReportYamlSyntaxError() {
        String content = "---\nname: [unclosed\n---\n# Title\n";
        FrontmatterResult result = parser.parse(content);
        assertTrue(result.hasError());
        assertTrue(result.errorMessage().contains("syntax error"));
    }

    @Test
    void parseShouldHandleNullContent() {
        FrontmatterResult result = parser.parse(null);
        assertFalse(result.hasError());
        assertFalse(result.hasFrontmatter());
    }

    @Test
    void parseShouldHandleContentStartingWithDashButNoNewline() {
        String content = "---";
        FrontmatterResult result = parser.parse(content);
        assertFalse(result.hasError());
        assertFalse(result.hasFrontmatter());
    }

    @Test
    void parseShouldCalculateCorrectBodyStartLine() {
        String content = "---\nname: test\nversion: \"1.0\"\n---\n# Title\n";
        FrontmatterResult result = parser.parse(content);
        // Opening --- (line 1) + 2 YAML lines + closing --- (line 4) → body starts at line 5
        assertEquals(5, result.bodyStartLine());
    }

    @Test
    void parseShouldHandleYamlNullLiteralInFrontmatter() {
        // YAML `~` (null literal) parses to NullNode → no data, no error
        String content = "---\n~\n---\n# Title\n";
        FrontmatterResult result = parser.parse(content);
        assertFalse(result.hasError());
        assertFalse(result.hasFrontmatter());
    }
}
