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

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TextNode;
import io.polychro.spi.Diagnostic;
import io.polychro.spi.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MarkdownLinkValidationIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void validateShouldReportBrokenRelativeLink() throws IOException {
        Path docFile = tempDir.resolve("doc.md");
        Files.writeString(docFile, "# Title\n\n[link](missing.md)\n");

        MarkdownValidator validator = new MarkdownValidator(
                120, "-", new GenericFormat(), new FrontmatterParser());

        Document doc = new Document(new TextNode(Files.readString(docFile)), docFile.toString());
        List<Diagnostic> diagnostics = validator.validate(doc);

        assertTrue(diagnostics.stream().anyMatch(d -> d.code().equals("broken-relative-link")));
    }

    @Test
    void validateShouldAcceptValidRelativeLink() throws IOException {
        Path target = tempDir.resolve("target.md");
        Files.writeString(target, "# Target\n");

        Path docFile = tempDir.resolve("doc.md");
        Files.writeString(docFile, "# Title\n\n[link](target.md)\n");

        MarkdownValidator validator = new MarkdownValidator(
                120, "-", new GenericFormat(), new FrontmatterParser());

        Document doc = new Document(new TextNode(Files.readString(docFile)), docFile.toString());
        List<Diagnostic> diagnostics = validator.validate(doc);

        assertTrue(diagnostics.stream().noneMatch(d -> d.code().equals("broken-relative-link")));
    }

    @Test
    void validateShouldReportBrokenRelativeAnchor() throws IOException {
        Path target = tempDir.resolve("target.md");
        Files.writeString(target, "# Title\n\n## Real Section\n");

        Path docFile = tempDir.resolve("doc.md");
        Files.writeString(docFile, "# Title\n\n[link](target.md#wrong-anchor)\n");

        MarkdownValidator validator = new MarkdownValidator(
                120, "-", new GenericFormat(), new FrontmatterParser());

        Document doc = new Document(new TextNode(Files.readString(docFile)), docFile.toString());
        List<Diagnostic> diagnostics = validator.validate(doc);

        assertTrue(diagnostics.stream().anyMatch(d -> d.code().equals("broken-relative-anchor")));
    }

    @Test
    void validateShouldNotCheckExternalLinksWhenDisabled() throws IOException {
        Path docFile = tempDir.resolve("doc.md");
        Files.writeString(docFile, "# Title\n\n[link](http://unreachable.invalid/page)\n");

        MarkdownValidator validator = new MarkdownValidator(
                120, "-", new GenericFormat(), new FrontmatterParser(),
                false, 5000, 10);

        Document doc = new Document(new TextNode(Files.readString(docFile)), docFile.toString());
        List<Diagnostic> diagnostics = validator.validate(doc);

        assertTrue(diagnostics.stream().noneMatch(d -> d.code().equals("broken-external-link")));
    }

    @Test
    void validateShouldCheckExternalLinksWhenEnabled() throws IOException {
        Path docFile = tempDir.resolve("doc.md");
        Files.writeString(docFile, "# Title\n\n[link](http://this-domain-definitely-does-not-exist-xyzzy.invalid/page)\n");

        MarkdownValidator validator = new MarkdownValidator(
                120, "-", new GenericFormat(), new FrontmatterParser(),
                true, 2000, 10);

        Document doc = new Document(new TextNode(Files.readString(docFile)), docFile.toString());
        List<Diagnostic> diagnostics = validator.validate(doc);

        assertTrue(diagnostics.stream().anyMatch(d -> d.code().equals("broken-external-link")));
    }

    @Test
    void validateShouldIgnoreMailtoLinks() throws IOException {
        Path docFile = tempDir.resolve("doc.md");
        Files.writeString(docFile, "# Title\n\n[email](mailto:user@example.com)\n");

        MarkdownValidator validator = new MarkdownValidator(
                120, "-", new GenericFormat(), new FrontmatterParser());

        Document doc = new Document(new TextNode(Files.readString(docFile)), docFile.toString());
        List<Diagnostic> diagnostics = validator.validate(doc);

        assertTrue(diagnostics.stream().noneMatch(d ->
                d.code().equals("broken-relative-link") || d.code().equals("broken-external-link")));
    }

    @Test
    void validateShouldSkipLinkChecksWhenNoSourcePath() {
        MarkdownValidator validator = new MarkdownValidator(
                120, "-", new GenericFormat(), new FrontmatterParser());

        Document doc = new Document(new TextNode("# Title\n\n[link](missing.md)\n"), null);
        List<Diagnostic> diagnostics = validator.validate(doc);

        // No broken-relative-link because source path is null (can't resolve)
        assertTrue(diagnostics.stream().noneMatch(d -> d.code().equals("broken-relative-link")));
    }

    @Test
    void validateShouldHandleMixedLinkTypes() throws IOException {
        Path target = tempDir.resolve("exists.md");
        Files.writeString(target, "# Exists\n\n## Section\n");

        Path docFile = tempDir.resolve("doc.md");
        Files.writeString(docFile, """
                # Title
                
                [valid relative](exists.md)
                [valid anchor](exists.md#section)
                [broken relative](missing.md)
                [internal](#title)
                """);

        MarkdownValidator validator = new MarkdownValidator(
                120, "-", new GenericFormat(), new FrontmatterParser());

        Document doc = new Document(new TextNode(Files.readString(docFile)), docFile.toString());
        List<Diagnostic> diagnostics = validator.validate(doc);

        long brokenRelative = diagnostics.stream()
                .filter(d -> d.code().equals("broken-relative-link")).count();
        assertEquals(1, brokenRelative);

        // No broken anchors or internal links
        assertTrue(diagnostics.stream().noneMatch(d -> d.code().equals("broken-relative-anchor")));
        assertTrue(diagnostics.stream().noneMatch(d -> d.code().equals("broken-internal-link")));
    }

        @Test
        void validateShouldCheckLinksNestedInListItems() throws IOException {
                Path target = tempDir.resolve("exists.md");
                Files.writeString(target, "# Exists\n");

                Path docFile = tempDir.resolve("doc.md");
                Files.writeString(docFile, "# Title\n\n- [valid](exists.md)\n- [broken](missing.md)\n");

                MarkdownValidator validator = new MarkdownValidator(
                                120, "-", new GenericFormat(), new FrontmatterParser());

                Document doc = new Document(new TextNode(Files.readString(docFile)), docFile.toString());
                List<Diagnostic> diagnostics = validator.validate(doc);

                long brokenRelative = diagnostics.stream()
                                .filter(d -> d.code().equals("broken-relative-link")).count();
                assertEquals(1, brokenRelative);
        }

    @Test
    void validateShouldIgnoreBlankDestinationLinks() throws IOException {
        // [text]() produces a blank destination — should not be collected as a link
        Path docFile = tempDir.resolve("doc.md");
        Files.writeString(docFile, "# Title\n\n[empty]()\n\n[also empty](  )\n");

        MarkdownValidator validator = new MarkdownValidator(
                120, "-", new GenericFormat(), new FrontmatterParser());

        Document doc = new Document(new TextNode(Files.readString(docFile)), docFile.toString());
        List<Diagnostic> diagnostics = validator.validate(doc);

        assertTrue(diagnostics.stream().noneMatch(d -> d.code().equals("broken-relative-link")));
    }

    @Test
    void validateShouldIgnoreMailtoAndTelLinks() throws IOException {
        Path docFile = tempDir.resolve("doc.md");
        Files.writeString(docFile, "# Title\n\n[email](mailto:a@b.com)\n\n[phone](tel:+123)\n");

        MarkdownValidator validator = new MarkdownValidator(
                120, "-", new GenericFormat(), new FrontmatterParser());

        Document doc = new Document(new TextNode(Files.readString(docFile)), docFile.toString());
        List<Diagnostic> diagnostics = validator.validate(doc);

        assertTrue(diagnostics.stream().noneMatch(d -> d.code().equals("broken-relative-link")));
        assertTrue(diagnostics.stream().noneMatch(d -> d.code().equals("broken-external-link")));
    }

    @Test
    void validateShouldClassifyHttpLinksAsExternal() throws IOException {
        Path docFile = tempDir.resolve("doc.md");
        Files.writeString(docFile, "# Title\n\n[link](http://example.com)\n\n[secure](https://example.com)\n");

        // External link checking disabled, so http:// links are just skipped
        MarkdownValidator validator = new MarkdownValidator(
                120, "-", new GenericFormat(), new FrontmatterParser());

        Document doc = new Document(new TextNode(Files.readString(docFile)), docFile.toString());
        List<Diagnostic> diagnostics = validator.validate(doc);

        // No broken-relative-link for http:// or https:// links
        assertTrue(diagnostics.stream().noneMatch(d -> d.code().equals("broken-relative-link")));
    }

    @Test
        void collectProjectedLinksShouldSkipBlankTarget() {
        MarkdownValidator validator = new MarkdownValidator(
                120, "-", new GenericFormat(), new FrontmatterParser());

                var root = JsonNodeFactory.instance.objectNode();
                root.putObject("document").putArray("links").addObject().put("target", "   ");
                Document projected = new Document(root, "markdown", null);

                List<MarkdownValidator.LinkInfo> links = validator.collectProjectedLinks(projected);
        assertTrue(links.isEmpty());
    }

        @Test
        void collectProjectedLinksShouldPreferBlockLinksWhenPresent() {
                MarkdownValidator validator = new MarkdownValidator(
                                120, "-", new GenericFormat(), new FrontmatterParser());

                var blockLink = JsonNodeFactory.instance.objectNode();
                blockLink.put("target", "target.md");
                var root = JsonNodeFactory.instance.objectNode();
                var document = root.putObject("document");
                document.putArray("blocks").addObject().putArray("links").add(blockLink);
                document.putArray("links").addObject().put("target", "legacy.md");
                Document projected = new Document(root, "markdown", null);

                List<MarkdownValidator.LinkInfo> links = validator.collectProjectedLinks(projected);
                assertEquals(1, links.size());
                assertEquals("target.md", links.getFirst().target());
        }

            @Test
            void collectProjectedLinksShouldSkipBlankBlockTarget() {
                MarkdownValidator validator = new MarkdownValidator(
                        120, "-", new GenericFormat(), new FrontmatterParser());

                var root = JsonNodeFactory.instance.objectNode();
                root.putObject("document").putArray("blocks").addObject().putArray("links").addObject().put("target", "  ");
                Document projected = new Document(root, "markdown", null);

                List<MarkdownValidator.LinkInfo> links = validator.collectProjectedLinks(projected);
                assertTrue(links.isEmpty());
            }

            @Test
            void collectProjectedLinksShouldFallbackToLegacyLinksWhenBlocksMissing() {
                MarkdownValidator validator = new MarkdownValidator(
                        120, "-", new GenericFormat(), new FrontmatterParser());

                var root = JsonNodeFactory.instance.objectNode();
                root.putObject("document").putArray("links").addObject().put("target", "legacy.md");
                Document projected = new Document(root, "markdown", null);

                List<MarkdownValidator.LinkInfo> links = validator.collectProjectedLinks(projected);
                assertEquals(1, links.size());
                assertEquals("legacy.md", links.getFirst().target());
            }

            @Test
            void collectProjectedInternalLinksShouldPreferBlockLinksWhenPresent() {
                MarkdownValidator validator = new MarkdownValidator(
                        120, "-", new GenericFormat(), new FrontmatterParser());

                var internalLink = JsonNodeFactory.instance.objectNode();
                internalLink.put("target", "#title");
                internalLink.put("kind", "internal-anchor");
                var externalLink = JsonNodeFactory.instance.objectNode();
                externalLink.put("target", "https://example.com");
                externalLink.put("kind", "external");
                var root = JsonNodeFactory.instance.objectNode();
                root.putObject("document").putArray("blocks")
                        .addObject().putArray("links").add(internalLink).add(externalLink);
                Document projected = new Document(root, "markdown", null);

                List<MarkdownValidator.ProjectedLinkInfo> links = validator.collectProjectedInternalLinks(projected);
                assertEquals(1, links.size());
                assertEquals("#title", links.getFirst().target());
                assertEquals("$.document.blocks[0].links[0]", links.getFirst().path());
            }

            @Test
            void collectProjectedInternalLinksShouldFallbackToLegacyLinksWhenBlocksMissing() {
                MarkdownValidator validator = new MarkdownValidator(
                        120, "-", new GenericFormat(), new FrontmatterParser());

                var root = JsonNodeFactory.instance.objectNode();
                var document = root.putObject("document");
                document.putArray("links")
                        .addObject().put("target", "#title").put("kind", "internal-anchor");
                document.withArray("links")
                        .addObject().put("target", "guide.md").put("kind", "relative");
                Document projected = new Document(root, "markdown", null);

                List<MarkdownValidator.ProjectedLinkInfo> links = validator.collectProjectedInternalLinks(projected);
                assertEquals(1, links.size());
                assertEquals("#title", links.getFirst().target());
                assertEquals("$.document.links[0]", links.getFirst().path());
            }

            @Test
            void collectProjectedLinksShouldIncludeListItemLinksWhenBlocksPresent() {
                MarkdownValidator validator = new MarkdownValidator(
                        120, "-", new GenericFormat(), new FrontmatterParser());

                var root = JsonNodeFactory.instance.objectNode();
                var document = root.putObject("document");
                document.putArray("blocks")
                        .addObject()
                        .putArray("items")
                        .addObject()
                        .putArray("links")
                        .addObject()
                        .put("target", "guide.md");
                Document projected = new Document(root, "markdown", null);

                List<MarkdownValidator.LinkInfo> links = validator.collectProjectedLinks(projected);
                assertEquals(1, links.size());
                assertEquals("guide.md", links.getFirst().target());
            }

            @Test
            void collectProjectedInternalLinksShouldIncludeListItemLinksWhenBlocksPresent() {
                MarkdownValidator validator = new MarkdownValidator(
                        120, "-", new GenericFormat(), new FrontmatterParser());

                var root = JsonNodeFactory.instance.objectNode();
                var document = root.putObject("document");
                document.putArray("blocks")
                        .addObject()
                        .putArray("items")
                        .addObject()
                        .putArray("links")
                        .addObject()
                        .put("target", "#title")
                        .put("kind", "internal-anchor");
                Document projected = new Document(root, "markdown", null);

                List<MarkdownValidator.ProjectedLinkInfo> links = validator.collectProjectedInternalLinks(projected);
                assertEquals(1, links.size());
                assertEquals("#title", links.getFirst().target());
                assertEquals("$.document.blocks[0].items[0].links[0]", links.getFirst().path());
            }
}
