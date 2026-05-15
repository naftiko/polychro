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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AnchorCollectorTest {

    private final AnchorCollector collector = new AnchorCollector(
            List.of(new MarkdownReferenceAdapter(), new HtmlReferenceAdapter()));

    @Test
    void shouldCollectMarkdownAnchorsFromHeadings() {
        Document md = TestDocuments.markdown("""
                {"document":{"blocks":[
                  {"type":"heading","level":1,"text":"Intro","anchor":"intro"},
                  {"type":"paragraph","text":"hi"},
                  {"type":"heading","level":2,"text":"Body","anchor":"body"}
                ],"links":[]}}""");
        List<Anchor> anchors = collector.collect(md);
        assertEquals(2, anchors.size());
        assertEquals("intro", anchors.get(0).id());
        assertEquals("heading", anchors.get(0).origin());
        assertEquals("body", anchors.get(1).id());
    }

    @Test
    void shouldSkipMarkdownHeadingsWithoutAnchor() {
        Document md = TestDocuments.markdown("""
                {"document":{"blocks":[
                  {"type":"heading","level":1,"text":"Intro"},
                  {"type":"heading","level":2,"text":"Body","anchor":""}
                ],"links":[]}}""");
        assertTrue(collector.collect(md).isEmpty());
    }

    @Test
    void shouldCollectHtmlAnchorsFromElementIds() {
        Document html = TestDocuments.html("""
                {"document":{"nodes":[
                  {"tag":"section","id":"intro","children":[
                    {"tag":"h1","id":"title","children":[]}
                  ]},
                  {"tag":"p","children":[]}
                ],"links":[]}}""");
        List<Anchor> anchors = collector.collect(html);
        assertEquals(2, anchors.size());
        assertEquals("intro", anchors.get(0).id());
        assertEquals("element", anchors.get(0).origin());
        assertEquals("title", anchors.get(1).id());
    }

    @Test
    void shouldReturnEmptyWhenNoAdapterSupportsTheDocument() {
        Document yaml = new Document(
                new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode(),
                "yaml", "foo.yml", null, Map.of());
        assertTrue(collector.collect(yaml).isEmpty());
    }

    @Test
    void collectByIdAcrossShouldGroupAcrossDocuments() {
        Document md = TestDocuments.markdown("""
                {"document":{"blocks":[
                  {"type":"heading","level":1,"text":"X","anchor":"intro"}
                ],"links":[]}}""");
        Document html = TestDocuments.html("""
                {"document":{"nodes":[
                  {"tag":"h1","id":"intro","children":[]},
                  {"tag":"h2","id":"body","children":[]}
                ],"links":[]}}""");
        Map<String, List<Anchor>> grouped = collector.collectByIdAcross(List.of(md, html));
        assertEquals(2, grouped.size());
        assertEquals(2, grouped.get("intro").size());
        assertEquals(1, grouped.get("body").size());
    }

    @Test
    void idsOfShouldReturnDistinctIdsPreservingOrder() {
        Document html = TestDocuments.html("""
                {"document":{"nodes":[
                  {"tag":"a","id":"x","children":[]},
                  {"tag":"a","id":"y","children":[]},
                  {"tag":"a","id":"x","children":[]}
                ],"links":[]}}""");
        Set<String> ids = collector.idsOf(html);
        assertEquals(List.of("x", "y"), List.copyOf(ids));
    }

    @Test
    void constructorShouldRejectNullAdapters() {
        assertThrows(IllegalArgumentException.class, () -> new AnchorCollector(null));
    }

    @Test
    void collectShouldRejectNullDocument() {
        assertThrows(IllegalArgumentException.class, () -> collector.collect(null));
    }

    @Test
    void collectByIdAcrossShouldRejectNull() {
        assertThrows(IllegalArgumentException.class, () -> collector.collectByIdAcross(null));
    }
}
