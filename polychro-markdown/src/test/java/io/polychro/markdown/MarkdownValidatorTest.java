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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import io.polychro.spi.Diagnostic;
import io.polychro.spi.Document;
import io.polychro.spi.Severity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownValidatorTest {

    private final MarkdownValidator validator = new MarkdownValidator(
            120, "-", new GenericFormat(), new FrontmatterParser());

    private Document doc(String content) {
        JsonNode root = new TextNode(content);
        return new Document(root, "test.md");
    }

    @Test
    void validateShouldReturnEmptyDiagnosticsForCleanDocument() {
        String content = "# Title\n\nSome text here.\n\n## Section\n\nMore text.\n";
        List<Diagnostic> result = validator.validate(doc(content));
        assertTrue(result.isEmpty(), "Expected no diagnostics but got: " + result);
    }

    @Test
    void validateShouldReportSkippedHeadingLevel() {
        String content = "# Title\n\n### Skipped\n";
        List<Diagnostic> result = validator.validate(doc(content));
        assertTrue(result.stream().anyMatch(d -> d.code().equals("heading-hierarchy")));
    }

    @Test
    void validateShouldReportBrokenInternalLink() {
        String content = "# Title\n\n[link](#nonexistent)\n";
        List<Diagnostic> result = validator.validate(doc(content));
        assertTrue(result.stream().anyMatch(d -> d.code().equals("broken-internal-link")));
    }

    @Test
    void validateShouldNotReportValidInternalLink() {
        String content = "# Title\n\n## My Section\n\n[link](#my-section)\n";
        List<Diagnostic> result = validator.validate(doc(content));
        assertTrue(result.stream().noneMatch(d -> d.code().equals("broken-internal-link")));
    }

    @Test
    void validateShouldReportCodeBlockWithoutLanguage() {
        String content = "# Title\n\n```\ncode\n```\n";
        List<Diagnostic> result = validator.validate(doc(content));
        assertTrue(result.stream().anyMatch(d -> d.code().equals("code-block-no-language")));
    }

    @Test
    void validateShouldNotReportCodeBlockWithLanguage() {
        String content = "# Title\n\n```java\ncode\n```\n";
        List<Diagnostic> result = validator.validate(doc(content));
        assertTrue(result.stream().noneMatch(d -> d.code().equals("code-block-no-language")));
    }

    @Test
    void validateShouldReportTrailingWhitespace() {
        String content = "# Title\n\nLine with trailing space \n";
        List<Diagnostic> result = validator.validate(doc(content));
        assertTrue(result.stream().anyMatch(d -> d.code().equals("trailing-whitespace")));
    }

    @Test
    void validateShouldNotReportTrailingWhitespaceOnCleanLine() {
        String content = "# Title\n\nClean line\n";
        List<Diagnostic> result = validator.validate(doc(content));
        assertTrue(result.stream().noneMatch(d -> d.code().equals("trailing-whitespace")));
    }

    @Test
    void validateShouldReportMixedListMarkers() {
        String content = "# Title\n\n- Item one\n* Item two\n";
        List<Diagnostic> result = validator.validate(doc(content));
        assertTrue(result.stream().anyMatch(d -> d.code().equals("inconsistent-list-marker")));
    }

    @Test
    void validateShouldNotReportConsistentListMarkers() {
        String content = "# Title\n\n- Item one\n- Item two\n";
        List<Diagnostic> result = validator.validate(doc(content));
        assertTrue(result.stream().noneMatch(d -> d.code().equals("inconsistent-list-marker")));
    }

    @Test
    void validateShouldReportMissingBlankLineBeforeHeading() {
        String content = "# Title\nSome text\n## Section\n";
        List<Diagnostic> result = validator.validate(doc(content));
        assertTrue(result.stream().anyMatch(d -> d.code().equals("no-blank-line-before-heading")));
    }

    @Test
    void validateShouldNotReportMissingBlankLineAfterFrontmatterDelimiter() {
        String content = "---\nname: test\n---\n# Title\n";
        List<Diagnostic> result = validator.validate(doc(content));
        assertTrue(result.stream().noneMatch(d -> d.code().equals("no-blank-line-before-heading")));
    }

    @Test
    void validateShouldReportDuplicateAnchors() {
        String content = "# Title\n\n## Section\n\n## Section\n";
        List<Diagnostic> result = validator.validate(doc(content));
        assertTrue(result.stream().anyMatch(d -> d.code().equals("duplicate-anchor")));
    }

    @Test
    void validateShouldReportNoContentForNullRoot() {
        Document nullDoc = new Document(null, "test.md");
        List<Diagnostic> result = validator.validate(nullDoc);
        assertEquals(1, result.size());
        assertEquals("no-content", result.getFirst().code());
    }

    @Test
    void validateShouldReportNoContentForNonTextualRoot() {
        JsonNode objectNode = new ObjectMapper().createObjectNode();
        Document objDoc = new Document(objectNode, "test.md");
        List<Diagnostic> result = validator.validate(objDoc);
        assertEquals(1, result.size());
        assertEquals("no-content", result.getFirst().code());
    }
}
