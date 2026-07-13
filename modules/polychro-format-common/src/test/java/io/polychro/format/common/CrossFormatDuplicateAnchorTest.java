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

import io.polychro.spi.Diagnostic;
import io.polychro.spi.Document;
import io.polychro.spi.Severity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CrossFormatDuplicateAnchorTest {

    private final AnchorCollector collector = new AnchorCollector(
            List.of(new MarkdownReferenceAdapter(), new HtmlReferenceAdapter()));

    @Test
    void noDuplicatesYieldsNoDiagnostics() {
        Document md = TestDocuments.markdown("""
                {"document":{"blocks":[
                  {"type":"heading","level":1,"text":"A","anchor":"a"},
                  {"type":"heading","level":2,"text":"B","anchor":"b"}
                ],"links":[]}}""");
        assertTrue(new DuplicateAnchorRule(collector).apply(md).isEmpty());
    }

    @Test
    void withinDocumentDuplicateIsReported() {
        Document md = TestDocuments.markdown("""
                {"document":{"blocks":[
                  {"type":"heading","level":1,"text":"A","anchor":"intro"},
                  {"type":"heading","level":2,"text":"B","anchor":"intro"}
                ],"links":[]}}""");
        List<Diagnostic> diagnostics = new DuplicateAnchorRule(collector).apply(md);
        assertEquals(1, diagnostics.size());
        Diagnostic only = diagnostics.get(0);
        assertEquals("duplicate-anchor", only.code());
        assertEquals(Severity.WARN, only.severity());
        assertTrue(only.message().contains("intro"));
        assertEquals("$.document.blocks[1].anchor", only.path());
    }

    @Test
    void htmlWithinDocumentDuplicateIsReported() {
        Document html = TestDocuments.html("""
                {"document":{"nodes":[
                  {"tag":"section","id":"x","children":[]},
                  {"tag":"section","id":"x","children":[]}
                ],"links":[]}}""");
        List<Diagnostic> diagnostics = new DuplicateAnchorRule(collector).apply(html);
        assertEquals(1, diagnostics.size());
    }

    @Test
    void crossDocumentDuplicateIsReportedWhenCompanionsProvided() {
        Document md = TestDocuments.markdown("""
                {"document":{"blocks":[
                  {"type":"heading","level":1,"text":"X","anchor":"intro"}
                ],"links":[]}}""");
        Document companion = TestDocuments.html("""
                {"document":{"nodes":[
                  {"tag":"h1","id":"intro","children":[]}
                ],"links":[]}}""");
        List<Diagnostic> diagnostics = new DuplicateAnchorRule(collector)
                .applyAcross(md, List.of(companion));
        assertEquals(1, diagnostics.size());
        assertEquals("$.document.blocks[0].anchor", diagnostics.get(0).path());
    }

    @Test
    void diagnosticsAreNotEmittedForCompanionDocumentItself() {
        // Two companions declare the same id; the subject doc declares a fresh id => no diag.
        Document md = TestDocuments.markdown("""
                {"document":{"blocks":[
                  {"type":"heading","level":1,"text":"Fresh","anchor":"fresh"}
                ],"links":[]}}""");
        Document c1 = TestDocuments.html("""
                {"document":{"nodes":[
                  {"tag":"h1","id":"shared","children":[]}
                ],"links":[]}}""");
        Document c2 = TestDocuments.html("""
                {"document":{"nodes":[
                  {"tag":"h1","id":"shared","children":[]}
                ],"links":[]}}""");
        List<Diagnostic> diagnostics = new DuplicateAnchorRule(collector)
                .applyAcross(md, List.of(c1, c2));
        assertTrue(diagnostics.isEmpty());
    }

    @Test
    void emptyCompanionListBehavesLikeApply() {
        Document md = TestDocuments.markdown("""
                {"document":{"blocks":[
                  {"type":"heading","level":1,"text":"A","anchor":"x"},
                  {"type":"heading","level":2,"text":"B","anchor":"x"}
                ],"links":[]}}""");
        DuplicateAnchorRule rule = new DuplicateAnchorRule(collector);
        assertEquals(rule.apply(md).size(), rule.applyAcross(md, List.of()).size());
    }

    @Test
    void constructorRejectsNullCollector() {
        assertThrows(IllegalArgumentException.class, () -> new DuplicateAnchorRule(null));
    }

    @Test
    void applyRejectsNullDocument() {
        DuplicateAnchorRule rule = new DuplicateAnchorRule(collector);
        assertThrows(IllegalArgumentException.class, () -> rule.apply(null));
    }

    @Test
    void applyAcrossRejectsNullCompanions() {
        DuplicateAnchorRule rule = new DuplicateAnchorRule(collector);
        Document md = TestDocuments.markdown("""
                {"document":{"blocks":[],"links":[]}}""");
        assertThrows(IllegalArgumentException.class, () -> rule.applyAcross(md, null));
    }
}
