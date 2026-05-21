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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Targeted tests for {@link MarkdownReferenceAdapter} that exercise the recursive traversal of
 * the canonical Markdown projection shape: anchors and link references can live both at the top
 * of the {@code blocks[*]} tree and nested arbitrarily deep inside list items.
 */
class MarkdownReferenceAdapterTest {

    private final MarkdownReferenceAdapter adapter = new MarkdownReferenceAdapter();

    @Test
    void anchorsAreCollectedFromTopLevelHeadingBlock() {
        Document md = TestDocuments.markdown("""
                {"document":{"blocks":[
                  {"type":"heading","level":1,"text":"Intro","anchor":"intro"}
                ]}}""");
        List<Anchor> anchors = adapter.anchors(md);
        assertEquals(1, anchors.size());
        assertEquals("intro", anchors.get(0).id());
        assertEquals("$.document.blocks[0].anchor", anchors.get(0).path());
        assertEquals("heading", anchors.get(0).origin());
    }

    @Test
    void anchorsAreCollectedFromHeadingsNestedInListItems() {
        Document md = TestDocuments.markdown("""
                {"document":{"blocks":[
                  {"type":"list","ordered":false,"marker":"-","items":[
                    {"text":"item","blocks":[
                      {"type":"heading","level":2,"text":"Nested","anchor":"nested"}
                    ]}
                  ]}
                ]}}""");
        List<Anchor> anchors = adapter.anchors(md);
        assertEquals(1, anchors.size());
        assertEquals("nested", anchors.get(0).id());
        assertEquals("$.document.blocks[0].items[0].blocks[0].anchor",
                anchors.get(0).path());
    }

    @Test
    void anchorsSkipHeadingsWithNullOrEmptyAnchor() {
        Document md = TestDocuments.markdown("""
                {"document":{"blocks":[
                  {"type":"heading","level":1,"text":"NoAnchor"},
                  {"type":"heading","level":2,"text":"Empty","anchor":""},
                  {"type":"heading","level":3,"text":"Has","anchor":"has"}
                ]}}""");
        List<Anchor> anchors = adapter.anchors(md);
        assertEquals(1, anchors.size());
        assertEquals("has", anchors.get(0).id());
    }

    @Test
    void referencesAreCollectedFromParagraphLinks() {
        Document md = TestDocuments.markdown("""
                {"document":{"blocks":[
                  {"type":"paragraph","text":"x","links":[
                    {"target":"#intro","kind":"internal-anchor","text":"jump"},
                    {"target":"./other.md","kind":"relative","text":"file"}
                  ]}
                ]}}""");
        List<LinkReference> refs = adapter.references(md);
        assertEquals(2, refs.size());
        assertEquals(LinkKind.INTERNAL_ANCHOR, refs.get(0).kind());
        assertEquals("intro", refs.get(0).fragment());
        assertEquals("$.document.blocks[0].links[0].target", refs.get(0).path());
        assertEquals(LinkKind.RELATIVE_FILE, refs.get(1).kind());
        assertEquals("$.document.blocks[0].links[1].target", refs.get(1).path());
    }

    @Test
    void referencesAreCollectedFromLinksNestedInsideListItems() {
        // Mirror the structure emitted by MarkdownProjector when a paragraph with a link sits
        // inside a bullet list item: the link is projected into the paragraph block under
        // blocks[i].items[j].blocks[k].links[l].
        Document md = TestDocuments.markdown("""
                {"document":{"blocks":[
                  {"type":"list","ordered":false,"marker":"-","items":[
                    {"text":"first","blocks":[
                      {"type":"paragraph","text":"first","links":[
                        {"target":"#one","kind":"internal-anchor","text":"a"}
                      ]}
                    ]},
                    {"text":"second","blocks":[
                      {"type":"list","ordered":false,"marker":"-","items":[
                        {"text":"deep","blocks":[
                          {"type":"paragraph","text":"deep","links":[
                            {"target":"#two","kind":"internal-anchor","text":"b"}
                          ]}
                        ]}
                      ]}
                    ]}
                  ]}
                ]}}""");
        List<LinkReference> refs = adapter.references(md);
        assertEquals(2, refs.size());
        assertEquals("one", refs.get(0).fragment());
        assertEquals(
                "$.document.blocks[0].items[0].blocks[0].links[0].target",
                refs.get(0).path());
        assertEquals("two", refs.get(1).fragment());
        assertEquals(
                "$.document.blocks[0].items[1].blocks[0].items[0].blocks[0].links[0].target",
                refs.get(1).path());
    }

    @Test
    void referencesPreserveLinkKindClassification() {
        Document md = TestDocuments.markdown("""
                {"document":{"blocks":[
                  {"type":"paragraph","text":"x","links":[
                    {"target":"#fragment","kind":"internal-anchor","text":""},
                    {"target":"https://example.com","kind":"external","text":""},
                    {"target":"./file.md","kind":"relative","text":""},
                    {"target":"mailto:x@y","kind":"mailto","text":""}
                  ]}
                ]}}""");
        List<LinkReference> refs = adapter.references(md);
        assertEquals(4, refs.size());
        assertEquals(LinkKind.INTERNAL_ANCHOR, refs.get(0).kind());
        assertEquals(LinkKind.EXTERNAL, refs.get(1).kind());
        assertEquals(LinkKind.RELATIVE_FILE, refs.get(2).kind());
        assertEquals(LinkKind.MAILTO, refs.get(3).kind());
    }

    @Test
    void anchorsAndReferencesAreEmptyWhenBlocksMissing() {
        Document md = TestDocuments.markdown("{\"document\":{}}");
        assertTrue(adapter.anchors(md).isEmpty());
        assertTrue(adapter.references(md).isEmpty());
    }

    @Test
    void anchorsAndReferencesIgnoreRootLevelLinksArray() {
        // The pre-refactor projection shape (links at $.document.links[*]) is no longer emitted
        // by MarkdownProjector. Documents that still carry it must not produce phantom references.
        Document md = TestDocuments.markdown("""
                {"document":{"blocks":[],"links":[
                  {"target":"#legacy","kind":"internal-anchor","text":""}
                ]}}""");
        assertTrue(adapter.anchors(md).isEmpty());
        assertTrue(adapter.references(md).isEmpty());
    }
}
