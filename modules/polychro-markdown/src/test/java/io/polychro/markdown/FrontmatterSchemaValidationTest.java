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
package io.polychro.markdown;

import com.fasterxml.jackson.databind.node.TextNode;
import io.polychro.spi.Diagnostic;
import io.polychro.spi.Document;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Additional coverage tests for edge cases in FrontmatterParser.
 */
class FrontmatterSchemaValidationTest {

    private final FrontmatterParser parser = new FrontmatterParser();

    @Test
    void parseShouldHandleClosingDelimiterWithNoTrailingNewline() {
        // content = "---\nname: test\n---" (no newline at end)
        String content = "---\nname: test\n---";
        FrontmatterResult result = parser.parse(content);
        assertFalse(result.hasError());
        assertTrue(result.hasFrontmatter());
        assertEquals("test", result.data().get("name").asText());
        assertEquals("", result.body());
    }

    @Test
    void parseShouldHandleOnlyDelimiterNoNewline() {
        // content = "---" (just the opening delimiter, nothing else)
        String content = "---";
        FrontmatterResult result = parser.parse(content);
        assertFalse(result.hasError());
        assertFalse(result.hasFrontmatter());
    }

    @Test
    void parseShouldHandleNullYamlValueInFrontmatter() {
        // YAML "null" literal parses to NullNode — handled gracefully (no data, no error)
        String content = "---\nnull\n---\n# Title\n";
        FrontmatterResult result = parser.parse(content);
        assertFalse(result.hasError());
        assertFalse(result.hasFrontmatter());
    }

    @Test
    void parseShouldReportScalarYamlAsFrontmatter() {
        // A plain scalar string is not a mapping
        String content = "---\nhello world\n---\n# Title\n";
        FrontmatterResult result = parser.parse(content);
        assertTrue(result.hasError());
        assertTrue(result.errorMessage().contains("mapping"));
    }

    @Test
    void parseShouldHandleFrontmatterWithOnlyWhitespace() {
        // frontmatter with only spaces/newlines
        String content = "---\n  \n---\n# Title\n";
        FrontmatterResult result = parser.parse(content);
        assertFalse(result.hasError());
        assertFalse(result.hasFrontmatter());
    }

    @Test
    void parseShouldHandleEmptyFrontmatterNoBodyContent() {
        // "---\n---" with no trailing content
        String content = "---\n---";
        FrontmatterResult result = parser.parse(content);
        assertFalse(result.hasError());
        assertFalse(result.hasFrontmatter());
    }

    @Test
    void validateShouldReportFrontmatterErrorInSkillFormat() {
        // SKILL.md with broken frontmatter
        MarkdownValidator validator = new MarkdownValidator(120, "-", new SkillFormat(), new FrontmatterParser());
        String content = "---\n[broken yaml\n---\n# Title\n";
        Document doc = new Document(new TextNode(content), "SKILL.md");
        List<Diagnostic> result = validator.validate(doc);
        assertTrue(result.stream().anyMatch(d -> d.code().equals("frontmatter-parse-error")));
    }

    @Test
    void validateShouldHandleSkillWithNullValueField() {
        MarkdownValidator validator = new MarkdownValidator(120, "-", new SkillFormat(), new FrontmatterParser());
        String content = "---\nname: null\ndescription: test\n---\n\n## Section\n";
        Document doc = new Document(new TextNode(content), "SKILL.md");
        List<Diagnostic> result = validator.validate(doc);
        // "null" as a string is valid, but YAML null literal is not
        // Jackson parses "null" as NullNode
        assertTrue(result.stream().anyMatch(d -> d.code().equals("skill-missing-field")));
    }

    @Test
    void hasFrontmatterShouldReturnFalseForMissingNode() {
        com.fasterxml.jackson.databind.node.MissingNode missing = com.fasterxml.jackson.databind.node.MissingNode.getInstance();
        FrontmatterResult result = new FrontmatterResult(missing, "body", 1, null);
        assertFalse(result.hasFrontmatter());
    }

    @Test
    void hasFrontmatterShouldReturnFalseForNullNode() {
        com.fasterxml.jackson.databind.node.NullNode nullNode = com.fasterxml.jackson.databind.node.NullNode.getInstance();
        FrontmatterResult result = new FrontmatterResult(nullNode, "body", 1, null);
        assertFalse(result.hasFrontmatter());
    }

    @Test
    void hasFrontmatterShouldReturnTrueForObjectNode() {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.node.ObjectNode obj = mapper.createObjectNode();
        obj.put("key", "value");
        FrontmatterResult result = new FrontmatterResult(obj, "body", 1, null);
        assertTrue(result.hasFrontmatter());
    }
}
