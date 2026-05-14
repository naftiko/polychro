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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Additional tests targeting uncovered branches in MarkdownValidator.
 */
class MarkdownValidatorEdgeCasesTest {

    private final MarkdownValidator validator = new MarkdownValidator(
            120, "-", new GenericFormat(), new FrontmatterParser());

    private Document doc(String content) {
        return new Document(new TextNode(content), "test.md");
    }

    @Test
    void slugifyShouldHandleCodeInHeading() {
        // Heading with backtick code spans: ## The `MyClass` type
        String content = "# Title\n\n## The `MyClass` Type\n\n[link](#the-myclass-type)\n";
        List<Diagnostic> result = validator.validate(doc(content));
        assertTrue(result.stream().noneMatch(d -> d.code().equals("broken-internal-link")));
    }

    @Test
    void validateShouldIgnoreHttpUrlOnLongLine() {
        String line = "http://example.com/" + "a".repeat(200);
        String content = "# Title\n\n" + line + "\n";
        List<Diagnostic> result = validator.validate(doc(content));
        assertTrue(result.stream().noneMatch(d -> d.code().equals("line-too-long")));
    }

    @Test
    void validateShouldNotExemptIndentedListItem() {
        // An indented line starting with "-" should NOT be exempted from line length
        String line = "    - " + "a".repeat(200);
        String content = "# Title\n\n" + line + "\n";
        MarkdownValidator shortValidator = new MarkdownValidator(50, "-", new GenericFormat(), new FrontmatterParser());
        List<Diagnostic> result = shortValidator.validate(doc(content));
        assertTrue(result.stream().anyMatch(d -> d.code().equals("line-too-long")));
    }

    @Test
    void validateShouldNotExemptIndentedStarListItem() {
        // An indented line starting with "*" should NOT be exempted from line length
        String line = "    * " + "a".repeat(200);
        String content = "# Title\n\n" + line + "\n";
        MarkdownValidator shortValidator = new MarkdownValidator(50, "-", new GenericFormat(), new FrontmatterParser());
        List<Diagnostic> result = shortValidator.validate(doc(content));
        assertTrue(result.stream().anyMatch(d -> d.code().equals("line-too-long")));
    }

    @Test
    void validateShouldReportPlusAsInconsistentListMarker() {
        String content = "# Title\n\n+ Item one\n";
        List<Diagnostic> result = validator.validate(doc(content));
        assertTrue(result.stream().anyMatch(d -> d.code().equals("inconsistent-list-marker")));
    }

    @Test
    void checkListMarkersShouldIgnoreNonListLines() {
        // Lines that don't start with list markers shouldn't trigger anything
        String content = "# Title\n\nSome text here.\n";
        List<Diagnostic> result = validator.validate(doc(content));
        assertTrue(result.stream().noneMatch(d -> d.code().equals("inconsistent-list-marker")));
    }

    @Test
    void checkListMarkersShouldIgnoreShortLines() {
        // Lines shorter than 2 chars after stripping shouldn't trigger
        String content = "# Title\n\n-\n";
        List<Diagnostic> result = validator.validate(doc(content));
        assertTrue(result.stream().noneMatch(d -> d.code().equals("inconsistent-list-marker")));
    }

    @Test
    void validateShouldAcceptBlankLineBeforeHeading() {
        String content = "# Title\n\nSome text.\n\n## Section\n";
        List<Diagnostic> result = validator.validate(doc(content));
        assertTrue(result.stream().noneMatch(d -> d.code().equals("no-blank-line-before-heading")));
    }

    @Test
    void validateShouldHandleEmptyLine() {
        // Empty lines should not trigger trailing whitespace
        String content = "# Title\n\n\nSome text.\n";
        List<Diagnostic> result = validator.validate(doc(content));
        assertTrue(result.stream().noneMatch(d -> d.code().equals("trailing-whitespace")));
    }

    @Test
    void validateShouldHandleDocumentWithNoHeadings() {
        String content = "Just some plain text.\n";
        List<Diagnostic> result = validator.validate(doc(content));
        assertTrue(result.stream().noneMatch(d -> d.code().equals("heading-hierarchy")));
        assertTrue(result.stream().noneMatch(d -> d.code().equals("duplicate-anchor")));
    }

    @Test
    void extractTextShouldHandleNestedNodesInHeading() {
        // CommonMark creates nested inline nodes for emphasis
        String text = MarkdownValidator.extractText(
                org.commonmark.parser.Parser.builder().build().parse("**bold** text").getFirstChild());
        assertTrue(text.contains("bold"));
        assertTrue(text.contains("text"));
    }

    @Test
    void getNodeLineShouldReturnBodyStartLineWhenNoSourceSpans() {
        // Create a node without source spans
        org.commonmark.node.Text textNode = new org.commonmark.node.Text("test");
        int line = MarkdownValidator.getNodeLine(textNode, 5);
        assertEquals(5, line);
    }

    @Test
    void validateShouldNotTreatStarWithoutSpaceAsListMarker() {
        // *bold* is emphasis, not a list
        String content = "# Title\n\n*bold text*\n";
        List<Diagnostic> result = validator.validate(doc(content));
        assertTrue(result.stream().noneMatch(d -> d.code().equals("inconsistent-list-marker")));
    }

    @Test
    void validateShouldNotTreatDashWithoutSpaceAsListMarker() {
        // --- is a horizontal rule, not a list
        String content = "# Title\n\n---\n\nText.\n";
        List<Diagnostic> result = validator.validate(doc(content));
        assertTrue(result.stream().noneMatch(d -> d.code().equals("inconsistent-list-marker")));
    }

    @Test
    void validateShouldReportCodeBlockWithBlankInfoString() {
        // Fenced code block with whitespace info (```  \n)
        String content = "# Title\n\n```  \ncode\n```\n";
        List<Diagnostic> result = validator.validate(doc(content));
        assertTrue(result.stream().anyMatch(d -> d.code().equals("code-block-no-language")));
    }

    @Test
    void validateShouldNotReportExternalLinks() {
        // External links should not be checked against headings
        String content = "# Title\n\n[ext](https://example.com)\n\n[internal](#title)\n";
        List<Diagnostic> result = validator.validate(doc(content));
        assertTrue(result.stream().noneMatch(d -> d.code().equals("broken-internal-link")));
    }

    @Test
    void checkCodeBlockLanguageShouldHandleNullInfo() {
        // Programmatically create a fenced code block with null info
        org.commonmark.node.FencedCodeBlock codeBlock = new org.commonmark.node.FencedCodeBlock();
        // info defaults to null when not set via setter
        org.commonmark.node.Document mdDoc = new org.commonmark.node.Document();
        mdDoc.appendChild(codeBlock);
        Document projected = new MarkdownProjector().project(
                new MarkdownParseResult("", new FrontmatterResult(null, "", 1, null), mdDoc),
                null);
        List<Diagnostic> diagnostics = new java.util.ArrayList<>();
        validator.checkCodeBlockLanguage(projected, diagnostics);
        assertEquals(1, diagnostics.size());
        assertEquals("code-block-no-language", diagnostics.getFirst().code());
    }

    @Test
    void checkCodeBlockLanguageShouldFallbackWhenSourceMapRangeIsMissing() {
        com.fasterxml.jackson.databind.node.ObjectNode root = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
        root.putObject("document").putArray("codeBlocks").addObject();

        List<Diagnostic> diagnostics = new java.util.ArrayList<>();
        validator.checkCodeBlockLanguage(new Document(root, "markdown", null), diagnostics);

        assertEquals(1, diagnostics.size());
        assertEquals(new io.polychro.spi.SourceRange(1, 1, 1, 1), diagnostics.getFirst().range());
    }

    @Test
    void checkCodeBlockLanguageShouldIgnoreProjectedBlockWithLanguage() {
        var root = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
        root.putObject("document").putArray("blocks")
                .addObject()
                .put("type", "code-block")
                .put("language", "java");

        List<Diagnostic> diagnostics = new java.util.ArrayList<>();
        validator.checkCodeBlockLanguage(new Document(root, "markdown", null), diagnostics);

        assertTrue(diagnostics.isEmpty());
    }

    @Test
    void checkCodeBlockLanguageShouldReportProjectedBlockWithNullLanguageNode() {
        var root = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
        root.putObject("document").putArray("blocks")
                .addObject()
                .put("type", "code-block")
                .putNull("language");

        List<Diagnostic> diagnostics = new java.util.ArrayList<>();
        validator.checkCodeBlockLanguage(new Document(root, "markdown", null), diagnostics);

        assertEquals(1, diagnostics.size());
        assertEquals("code-block-no-language", diagnostics.getFirst().code());
    }

    @Test
    void checkCodeBlockLanguageShouldReportProjectedBlockWithMissingLanguage() {
        var root = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
        root.putObject("document").putArray("blocks")
                .addObject()
                .put("type", "code-block");

        List<Diagnostic> diagnostics = new java.util.ArrayList<>();
        validator.checkCodeBlockLanguage(new Document(root, "markdown", null), diagnostics);

        assertEquals(1, diagnostics.size());
        assertEquals("code-block-no-language", diagnostics.getFirst().code());
    }

    @Test
    void checkCodeBlockLanguageShouldHandleLegacyBlankLanguage() {
        var root = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
        root.putObject("document").putArray("codeBlocks")
                .addObject()
                .put("language", "   ");

        List<Diagnostic> diagnostics = new java.util.ArrayList<>();
        validator.checkCodeBlockLanguage(new Document(root, "markdown", null), diagnostics);

        assertEquals(1, diagnostics.size());
        assertEquals("code-block-no-language", diagnostics.getFirst().code());
    }

    @Test
    void checkCodeBlockLanguageShouldHandleLegacyNullLanguageNode() {
        var root = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
        root.putObject("document").putArray("codeBlocks")
                .addObject()
                .putNull("language");

        List<Diagnostic> diagnostics = new java.util.ArrayList<>();
        validator.checkCodeBlockLanguage(new Document(root, "markdown", null), diagnostics);

        assertEquals(1, diagnostics.size());
        assertEquals("code-block-no-language", diagnostics.getFirst().code());
    }

    @Test
    void checkCodeBlockLanguageShouldIgnoreLegacyCodeBlockWithLanguage() {
        var root = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
        root.putObject("document").putArray("codeBlocks")
                .addObject()
                .put("language", "yaml");

        List<Diagnostic> diagnostics = new java.util.ArrayList<>();
        validator.checkCodeBlockLanguage(new Document(root, "markdown", null), diagnostics);

        assertTrue(diagnostics.isEmpty());
    }

    @Test
    void collectProjectedHeadingsShouldFallbackToLegacyHeadingsWhenBlocksMissing() {
        var heading = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
        heading.put("level", 2);
        heading.put("text", "Overview");
        var root = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
        root.putObject("document").putArray("headings").add(heading);

        var headings = validator.collectProjectedHeadings(new Document(root, "markdown", null));

        assertEquals(1, headings.size());
        assertEquals(2, headings.getFirst().level());
        assertEquals("Overview", headings.getFirst().text());
        assertEquals("$.document.headings[0]", headings.getFirst().path());
    }
}
