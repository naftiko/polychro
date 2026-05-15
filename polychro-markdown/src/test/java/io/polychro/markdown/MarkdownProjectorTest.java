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
        assertEquals("heading", projected.root().path("document").path("blocks").get(0).path("type").asText());
        assertEquals("Title", projected.root().path("document").path("blocks").get(0).path("text").asText());
        assertEquals("list", projected.root().path("document").path("blocks").get(1).path("type").asText());
        assertEquals("-", projected.root().path("document").path("blocks").get(1).path("marker").asText());
        assertEquals("Item", projected.root().path("document").path("blocks").get(1).path("items").get(0).path("text").asText());
        assertEquals("paragraph", projected.root().path("document").path("blocks").get(2).path("type").asText());
        assertEquals("link", projected.root().path("document").path("blocks").get(2).path("text").asText());
        assertEquals("#title", projected.root().path("document").path("blocks").get(2).path("links").get(0).path("target").asText());
        assertEquals("internal-anchor", projected.root().path("document").path("blocks").get(2).path("links").get(0).path("kind").asText());
        assertEquals("code-block", projected.root().path("document").path("blocks").get(3).path("type").asText());
        assertEquals("Title", projected.root().path("document").path("blocks").get(0).path("text").asText());
        assertEquals("title", projected.root().path("document").path("blocks").get(0).path("anchor").asText());
        assertEquals("", projected.root().path("document").path("blocks").get(3).path("language").asText());
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
        assertEquals(new SourceRange(5, 1, 5, 1), projected.sourceMap().resolve("$.document.blocks[0]"));
        assertEquals(new SourceRange(7, 1, 7, 1), projected.sourceMap().resolve("$.document.blocks[1]"));
        assertEquals(new SourceRange(9, 1, 9, 1), projected.sourceMap().resolve("$.document.blocks[2]"));
        assertEquals(new SourceRange(11, 1, 11, 1), projected.sourceMap().resolve("$.document.blocks[3]"));
        assertEquals(new SourceRange(9, 1, 9, 1), projected.sourceMap().resolve("$.document.blocks[2].links[0]"));
        assertNull(projected.sourceMap().resolve("$.document.headings[0]"));
        assertNull(projected.sourceMap().resolve("$.document.codeBlocks[0]"));
        assertNull(projected.sourceMap().resolve("$.document.lists[0]"));
        assertNull(projected.sourceMap().resolve("$.document.links[0]"));
    }

    @Test
    void projectShouldExposeOrderedListsAsBlocks() {
        MarkdownParseResult parsed = parserFacade.parse("""
                3. Third
                4. Fourth
                """);

        Document projected = projector.project(parsed, null);

        assertEquals("list", projected.root().path("document").path("blocks").get(0).path("type").asText());
        assertEquals(true, projected.root().path("document").path("blocks").get(0).path("ordered").asBoolean());
        assertEquals(".", projected.root().path("document").path("blocks").get(0).path("marker").asText());
        assertEquals(3, projected.root().path("document").path("blocks").get(0).path("startNumber").asInt());
        assertEquals("Third", projected.root().path("document").path("blocks").get(0).path("items").get(0).path("text").asText());
    }

    @Test
    void extractListItemTextShouldIgnoreNestedListText() {
        MarkdownParseResult parsed = parserFacade.parse("""
                - Parent
                  - Child
                """);

        org.commonmark.node.BulletList list = (org.commonmark.node.BulletList) parsed.bodyDocument().getFirstChild();
        org.commonmark.node.ListItem item = (org.commonmark.node.ListItem) list.getFirstChild();

        assertEquals("Parent", projector.extractListItemText(item));
    }

    @Test
    void extractListItemTextShouldJoinMultipleParagraphs() {
        org.commonmark.node.ListItem listItem = new org.commonmark.node.ListItem();
        org.commonmark.node.Paragraph first = new org.commonmark.node.Paragraph();
        first.appendChild(new org.commonmark.node.Text("First"));
        org.commonmark.node.Paragraph second = new org.commonmark.node.Paragraph();
        second.appendChild(new org.commonmark.node.Text("Second"));
        listItem.appendChild(first);
        listItem.appendChild(second);

        assertEquals("First\nSecond", projector.extractListItemText(listItem));
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

        assertEquals(0, projected.root().path("document").path("blocks").get(0).path("links").size());
    }

    @Test
    void projectShouldClassifyRelativeLinks() {
        MarkdownParseResult parsed = parserFacade.parse("[guide](docs/guide.md)\n");

        Document projected = projector.project(parsed, "docs/example.md");

        assertEquals("relative", projected.root().path("document").path("blocks").get(0).path("links").get(0).path("kind").asText());
    }

    @Test
    void appendListItemsShouldIgnoreNonListItemChildren() {
        org.commonmark.node.BulletList bulletList = new org.commonmark.node.BulletList();
        bulletList.appendChild(new org.commonmark.node.Paragraph());

        var items = JsonNodeFactory.instance.arrayNode();
        projector.appendListItems(bulletList, items, "$.document.blocks[0]",
                new MarkdownSourceMapBuilder(), 1);

        assertEquals(0, items.size());
    }
}