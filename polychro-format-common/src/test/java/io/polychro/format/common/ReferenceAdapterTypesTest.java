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
package io.polychro.format.common;

import io.polychro.spi.Document;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReferenceAdapterTypesTest {

    @Test
    void anchorRecordRejectsNulls() {
        assertThrows(IllegalArgumentException.class, () -> new Anchor(null, "$.x", "heading"));
        assertThrows(IllegalArgumentException.class, () -> new Anchor("a", null, "heading"));
        Anchor anchor = new Anchor("id", "$.x", null);
        assertEquals("id", anchor.id());
        assertEquals("$.x", anchor.path());
        assertNull(anchor.origin());
    }

    @Test
    void linkReferenceRejectsNulls() {
        assertThrows(IllegalArgumentException.class,
                () -> new LinkReference(null, LinkKind.EMPTY, "$.x", null, ""));
        assertThrows(IllegalArgumentException.class,
                () -> new LinkReference("x", null, "$.x", null, ""));
        assertThrows(IllegalArgumentException.class,
                () -> new LinkReference("x", LinkKind.EMPTY, null, null, ""));
        assertThrows(IllegalArgumentException.class,
                () -> new LinkReference("x", LinkKind.EMPTY, "$.x", null, null));
    }

    @Test
    void linkKindValuesAreExhaustive() {
        // touch every enum constant so they're loaded
        LinkKind[] values = LinkKind.values();
        assertEquals(9, values.length);
        assertEquals(LinkKind.EMPTY, LinkKind.valueOf("EMPTY"));
        assertEquals(LinkKind.MALFORMED, LinkKind.valueOf("MALFORMED"));
        assertEquals(LinkKind.INTERNAL_ANCHOR, LinkKind.valueOf("INTERNAL_ANCHOR"));
        assertEquals(LinkKind.RELATIVE_FILE, LinkKind.valueOf("RELATIVE_FILE"));
        assertEquals(LinkKind.EXTERNAL, LinkKind.valueOf("EXTERNAL"));
        assertEquals(LinkKind.MAILTO, LinkKind.valueOf("MAILTO"));
        assertEquals(LinkKind.TEL, LinkKind.valueOf("TEL"));
        assertEquals(LinkKind.DATA, LinkKind.valueOf("DATA"));
        assertEquals(LinkKind.JAVASCRIPT, LinkKind.valueOf("JAVASCRIPT"));
    }

    @Test
    void probeResultStatusValuesAreExhaustive() {
        ProbeResult.Status[] values = ProbeResult.Status.values();
        assertEquals(6, values.length);
        for (ProbeResult.Status s : values) {
            assertEquals(s, ProbeResult.Status.valueOf(s.name()));
        }
    }

    @Test
    void markdownAdapterRejectsWrongFormatOrShape() {
        MarkdownReferenceAdapter adapter = new MarkdownReferenceAdapter();
        assertFalse(adapter.supports(null));
        Document html = TestDocuments.html("""
                {"document":{"nodes":[],"links":[]}}""");
        assertFalse(adapter.supports(html));

        Document mdWithoutRoot = new Document(null, "markdown", "f.md", null, Map.of());
        assertFalse(adapter.supports(mdWithoutRoot));

        Document mdScalarRoot = new Document(
                com.fasterxml.jackson.databind.node.TextNode.valueOf("raw"),
                "markdown", "f.md", null, Map.of());
        assertFalse(adapter.supports(mdScalarRoot));

        // Supported doc but missing blocks/links arrays still yields empty lists.
        Document mdEmpty = TestDocuments.markdown("{\"document\":{}}");
        assertTrue(adapter.anchors(mdEmpty).isEmpty());
        assertTrue(adapter.references(mdEmpty).isEmpty());
    }

    @Test
    void htmlAdapterRejectsWrongFormatOrShape() {
        HtmlReferenceAdapter adapter = new HtmlReferenceAdapter();
        assertFalse(adapter.supports(null));
        Document md = TestDocuments.markdown("""
                {"document":{"blocks":[],"links":[]}}""");
        assertFalse(adapter.supports(md));

        Document htmlScalarRoot = new Document(
                com.fasterxml.jackson.databind.node.TextNode.valueOf("raw"),
                "html", "f.html", null, Map.of());
        assertFalse(adapter.supports(htmlScalarRoot));

        Document htmlNoRoot = new Document(null, "html", "f.html", null, Map.of());
        assertFalse(adapter.supports(htmlNoRoot));

        Document htmlEmpty = TestDocuments.html("{\"document\":{}}");
        assertTrue(adapter.anchors(htmlEmpty).isEmpty());
        assertTrue(adapter.references(htmlEmpty).isEmpty());
    }

    @Test
    void htmlAdapterSkipsElementsWithEmptyOrMissingId() {
        HtmlReferenceAdapter adapter = new HtmlReferenceAdapter();
        Document html = TestDocuments.html("""
                {"document":{"nodes":[
                  {"tag":"p","children":[]},
                  {"tag":"p","id":"","children":[]},
                  {"tag":"p","id":"keep","children":[]}
                ],"links":[]}}""");
        List<Anchor> anchors = adapter.anchors(html);
        assertEquals(1, anchors.size());
        assertEquals("keep", anchors.get(0).id());
    }

    @Test
    void htmlAdapterHandlesNodesWithoutChildrenKey() {
        HtmlReferenceAdapter adapter = new HtmlReferenceAdapter();
        Document html = TestDocuments.html("""
                {"document":{"nodes":[
                  {"tag":"br","id":"a"}
                ],"links":[]}}""");
        List<Anchor> anchors = adapter.anchors(html);
        assertEquals(1, anchors.size());
        assertEquals("a", anchors.get(0).id());
    }
}
