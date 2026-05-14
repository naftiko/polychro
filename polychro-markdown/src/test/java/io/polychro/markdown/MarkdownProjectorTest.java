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

import io.polychro.spi.Document;
import io.polychro.spi.SourceRange;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class MarkdownProjectorTest {

    private final MarkdownParserFacade parserFacade = new MarkdownParserFacade(new FrontmatterParser());
    private final MarkdownProjector projector = new MarkdownProjector();

    @Test
    void projectShouldExposeFrontmatterAndStructuralNodes() {
        MarkdownParseResult parsed = parserFacade.parse("""
                ---
                name: demo
                ---

                # Title

                - Item

                [link](#title)

                ```
                value
                ```
                """);

        Document projected = projector.project(parsed, "docs/example.md");

        assertEquals("markdown", projected.format());
        assertEquals("demo", projected.root().path("document").path("frontmatter").path("name").asText());
        assertEquals("Title", projected.root().path("document").path("headings").get(0).path("text").asText());
        assertEquals("title", projected.root().path("document").path("headings").get(0).path("anchor").asText());
        assertEquals("-", projected.root().path("document").path("lists").get(0).path("marker").asText());
        assertEquals("internal-anchor", projected.root().path("document").path("links").get(0).path("kind").asText());
        assertEquals("", projected.root().path("document").path("codeBlocks").get(0).path("language").asText());
    }

    @Test
    void sourceMapShouldResolveProjectedNodesBackToMarkdownLines() {
        MarkdownParseResult parsed = parserFacade.parse("""
                ---
                name: demo
                ---

                # Title

                * Item

                [link](#title)

                ```yaml
                value: demo
                ```
                """);

        Document projected = projector.project(parsed, "docs/example.md");

        assertEquals(new SourceRange(1, 1, 3, 1), projected.sourceMap().resolve("$.document.frontmatter"));
        assertEquals(new SourceRange(5, 1, 5, 1), projected.sourceMap().resolve("$.document.headings[0]"));
        assertEquals(new SourceRange(7, 1, 7, 1), projected.sourceMap().resolve("$.document.lists[0]"));
        assertEquals(new SourceRange(9, 1, 9, 1), projected.sourceMap().resolve("$.document.links[0]"));
        assertEquals(new SourceRange(11, 1, 11, 1), projected.sourceMap().resolve("$.document.codeBlocks[0]"));
    }

    @Test
    void projectShouldUseNullFrontmatterWhenMarkdownHasNone() {
        MarkdownParseResult parsed = parserFacade.parse("# Title\n");

        Document projected = projector.project(parsed, null);

        assertNotNull(projected.root().path("document").path("frontmatter"));
        assertEquals(true, projected.root().path("document").path("frontmatter").isNull());
        assertNull(projected.sourceMap().resolve("$.document.frontmatter"));
    }

    @Test
    void projectShouldSkipLinksWithNullDestination() {
        org.commonmark.node.Link link = new org.commonmark.node.Link(null, null);
        org.commonmark.node.Document body = new org.commonmark.node.Document();
        org.commonmark.node.Paragraph paragraph = new org.commonmark.node.Paragraph();
        paragraph.appendChild(link);
        body.appendChild(paragraph);

        Document projected = projector.project(
                new MarkdownParseResult("", new FrontmatterResult(null, "", 1, null), body),
                null);

        assertEquals(0, projected.root().path("document").path("links").size());
    }

    @Test
    void projectShouldClassifyRelativeLinks() {
        MarkdownParseResult parsed = parserFacade.parse("[guide](docs/guide.md)\n");

        Document projected = projector.project(parsed, "docs/example.md");

        assertEquals("relative", projected.root().path("document").path("links").get(0).path("kind").asText());
    }
}