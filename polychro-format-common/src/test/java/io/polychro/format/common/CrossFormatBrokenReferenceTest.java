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
import io.polychro.spi.SourceRange;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CrossFormatBrokenReferenceTest {

    @Test
    void markdownInternalAnchorMustResolve() {
        Document md = TestDocuments.markdown("""
                {"document":{"blocks":[
                  {"type":"heading","level":1,"text":"Intro","anchor":"intro"}
                ],"links":[
                  {"target":"#intro","kind":"internal-anchor","text":"jump"},
                  {"target":"#missing","kind":"internal-anchor","text":"broken"}
                ]}}""");
        BrokenLocalReferenceRule rule = new BrokenLocalReferenceRule(
                new MarkdownReferenceAdapter(), path -> true);
        List<Diagnostic> diagnostics = rule.apply(md);
        assertEquals(1, diagnostics.size());
        Diagnostic only = diagnostics.get(0);
        assertEquals("broken-local-reference", only.code());
        assertEquals(Severity.WARN, only.severity());
        assertTrue(only.message().contains("#missing"));
    }

    @Test
    void htmlInternalAnchorMustResolve() {
        Document html = TestDocuments.html("""
                {"document":{"nodes":[
                  {"tag":"h1","id":"intro","children":[]}
                ],"links":[
                  {"tag":"a","href":"#intro","text":"jump","kind":"fragment"},
                  {"tag":"a","href":"#missing","text":"broken","kind":"fragment"}
                ]}}""");
        BrokenLocalReferenceRule rule = new BrokenLocalReferenceRule(
                new HtmlReferenceAdapter(), path -> true);
        List<Diagnostic> diagnostics = rule.apply(html);
        assertEquals(1, diagnostics.size());
        assertTrue(diagnostics.get(0).message().contains("#missing"));
    }

    @Test
    void sameBrokenLinkDetectedIdenticallyFromMarkdownAndHtml() {
        Document md = TestDocuments.markdown("""
                {"document":{"blocks":[],"links":[
                  {"target":"#nowhere","kind":"internal-anchor","text":""}
                ]}}""");
        Document html = TestDocuments.html("""
                {"document":{"nodes":[],"links":[
                  {"tag":"a","href":"#nowhere","text":"","kind":"fragment"}
                ]}}""");
        List<Diagnostic> mdDiags = new BrokenLocalReferenceRule(
                new MarkdownReferenceAdapter(), p -> true).apply(md);
        List<Diagnostic> htmlDiags = new BrokenLocalReferenceRule(
                new HtmlReferenceAdapter(), p -> true).apply(html);
        assertEquals(1, mdDiags.size());
        assertEquals(1, htmlDiags.size());
        assertEquals(mdDiags.get(0).code(), htmlDiags.get(0).code());
        assertEquals(mdDiags.get(0).severity(), htmlDiags.get(0).severity());
        assertEquals(mdDiags.get(0).message(), htmlDiags.get(0).message());
    }

    @Test
    void anchorWithoutFragmentIsTreatedAsBroken() {
        // Anchor reference "#" yields a fragment of null, which cannot match any declared id.
        Document md = TestDocuments.markdown("""
                {"document":{"blocks":[
                  {"type":"heading","level":1,"text":"X","anchor":"x"}
                ],"links":[
                  {"target":"#","kind":"internal-anchor","text":""}
                ]}}""");
        List<Diagnostic> diagnostics = new BrokenLocalReferenceRule(
                new MarkdownReferenceAdapter(), p -> true).apply(md);
        assertEquals(1, diagnostics.size());
    }

    @Test
    void relativeFileMustExistOnDisk() {
        Document md = TestDocuments.markdown("""
                {"document":{"blocks":[],"links":[
                  {"target":"./present.md","kind":"relative","text":""},
                  {"target":"./missing.md","kind":"relative","text":""}
                ]}}""", "docs/parent.md");
        Set<String> existing = Set.of(
                java.nio.file.Path.of("docs").resolve("present.md").normalize().toString());
        List<Diagnostic> diagnostics = new BrokenLocalReferenceRule(
                new MarkdownReferenceAdapter(),
                p -> existing.contains(p.toString())).apply(md);
        assertEquals(1, diagnostics.size());
        assertTrue(diagnostics.get(0).message().contains("missing.md"));
    }

    @Test
    void relativeFileChecksUseCurrentDirectoryWhenSourcePathHasNoParent() {
        Document md = TestDocuments.markdown("""
                {"document":{"blocks":[],"links":[
                  {"target":"present.md","kind":"relative","text":""}
                ]}}""", "parent.md");
        java.util.concurrent.atomic.AtomicReference<java.nio.file.Path> probed =
                new java.util.concurrent.atomic.AtomicReference<>();
        new BrokenLocalReferenceRule(
                new MarkdownReferenceAdapter(),
                p -> { probed.set(p); return true; }).apply(md);
        assertEquals(java.nio.file.Path.of("present.md").normalize(),
                probed.get());
    }

    @Test
    void relativeFileCheckSkippedWhenDocumentHasNoSourcePath() {
        Document md = TestDocuments.markdown("""
                {"document":{"blocks":[],"links":[
                  {"target":"./x.md","kind":"relative","text":""}
                ]}}""", null);
        List<Diagnostic> diagnostics = new BrokenLocalReferenceRule(
                new MarkdownReferenceAdapter(), p -> false).apply(md);
        assertTrue(diagnostics.isEmpty());
    }

    @Test
    void externalAndSpecialSchemesAreIgnored() {
        Document html = TestDocuments.html("""
                {"document":{"nodes":[],"links":[
                  {"tag":"a","href":"https://example.com","text":"x","kind":"external"},
                  {"tag":"a","href":"mailto:x@y.com","text":"x","kind":"mailto"},
                  {"tag":"a","href":"javascript:void(0)","text":"x","kind":"javascript"}
                ]}}""");
        List<Diagnostic> diagnostics = new BrokenLocalReferenceRule(
                new HtmlReferenceAdapter(), p -> false).apply(html);
        assertTrue(diagnostics.isEmpty());
    }

    @Test
    void unsupportedDocumentYieldsNoDiagnostics() {
        Document yaml = new Document(
                new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode(),
                "yaml", "x.yml", null, Map.of());
        List<Diagnostic> diagnostics = new BrokenLocalReferenceRule(
                new MarkdownReferenceAdapter(), p -> true).apply(yaml);
        assertTrue(diagnostics.isEmpty());
    }

    @Test
    void sourceMapIsConsultedForRange() {
        SourceRange expected = new SourceRange(5, 1, 5, 12);
        Document base = TestDocuments.markdown("""
                {"document":{"blocks":[],"links":[
                  {"target":"#missing","kind":"internal-anchor","text":""}
                ]}}""");
        Document md = TestDocuments.withSourceMap(base,
                Map.of("$.document.links[0].target", expected));
        List<Diagnostic> diagnostics = new BrokenLocalReferenceRule(
                new MarkdownReferenceAdapter(), p -> true).apply(md);
        assertEquals(expected, diagnostics.get(0).range());
    }

    @Test
    void constructorRejectsNullAdapter() {
        assertThrows(IllegalArgumentException.class,
                () -> new BrokenLocalReferenceRule(null));
        assertThrows(IllegalArgumentException.class,
                () -> new BrokenLocalReferenceRule(new MarkdownReferenceAdapter(), null));
        assertThrows(IllegalArgumentException.class,
                () -> new BrokenLocalReferenceRule(null, p -> true));
    }

    @Test
    void applyRejectsNullDocument() {
        BrokenLocalReferenceRule rule = new BrokenLocalReferenceRule(new MarkdownReferenceAdapter());
        assertThrows(IllegalArgumentException.class, () -> rule.apply(null));
    }

    @Test
    void defaultConstructorUsesFilesExistsAndCompiles() {
        // Smoke-test the convenience constructor (no need to actually hit the FS).
        BrokenLocalReferenceRule rule = new BrokenLocalReferenceRule(new MarkdownReferenceAdapter());
        assertNotNull(rule);
    }
}
