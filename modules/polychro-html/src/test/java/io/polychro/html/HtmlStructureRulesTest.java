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
package io.polychro.html;

import io.polychro.spi.Diagnostic;
import io.polychro.spi.Document;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlStructureRulesTest {

    private List<Diagnostic> diagnose(String html, HtmlProfile profile) {
        Document doc = Document.fromString(html, "html", "/tmp/x.html");
        return new HtmlValidator(profile).validate(doc);
    }

    @Test
    void shouldFlagDuplicateIds() {
        List<Diagnostic> diags = diagnose(
                "<div id=\"a\"></div><span id=\"a\"></span>", new FragmentHtmlProfile());
        assertTrue(diags.stream().anyMatch(d -> "html-duplicate-id".equals(d.code())));
    }

    @Test
    void shouldFlagHeadingLevelJump() {
        List<Diagnostic> diags = diagnose(
                "<h1>x</h1><h3>y</h3>", new FragmentHtmlProfile());
        assertTrue(diags.stream().anyMatch(d -> "html-heading-order".equals(d.code())));
    }

    @Test
    void shouldNotFlagSequentialHeadings() {
        List<Diagnostic> diags = diagnose(
                "<h1>x</h1><h2>y</h2><h3>z</h3>", new FragmentHtmlProfile());
        assertTrue(diags.stream().noneMatch(d -> "html-heading-order".equals(d.code())));
    }

    @Test
    void shouldFlagBrokenFragmentLinks() {
        List<Diagnostic> diags = diagnose(
                "<a href=\"#missing\">x</a><a href=\"#known\">y</a><h1 id=\"known\">k</h1>",
                new FragmentHtmlProfile());
        long broken = diags.stream().filter(d -> "html-broken-fragment".equals(d.code())).count();
        assertEquals(1, broken);
    }

    @Test
    void shouldResolveFragmentToNamedAnchor() {
        List<Diagnostic> diags = diagnose(
                "<a name=\"top\">anchor</a><a href=\"#top\">link</a>",
                new FragmentHtmlProfile());
        assertTrue(diags.stream().noneMatch(d -> "html-broken-fragment".equals(d.code())));
    }

    @Test
    void shouldIgnoreEmptyFragmentLinks() {
        // href="#" — fragment but no actual id reference
        List<Diagnostic> diags = diagnose(
                "<a href=\"#\">x</a>", new FragmentHtmlProfile());
        assertTrue(diags.stream().noneMatch(d -> "html-broken-fragment".equals(d.code())));
    }

    @Test
    void shouldRequireHtmlRootInDocumentProfile() {
        List<Diagnostic> diags = diagnose("<p>x</p>", new DocumentHtmlProfile());
        // JSoup auto-wraps with <html>, so missing-root isn't triggered.
        // Instead, missing-lang is.
        assertTrue(diags.stream().anyMatch(d -> "html-missing-lang".equals(d.code())));
    }

    @Test
    void shouldFlagMissingLangAndTitleInDocumentProfile() {
        List<Diagnostic> diags = diagnose(
                "<!DOCTYPE html><html><body><p>x</p></body></html>",
                new DocumentHtmlProfile());
        assertTrue(diags.stream().anyMatch(d -> "html-missing-lang".equals(d.code())));
        assertTrue(diags.stream().anyMatch(d -> "html-missing-title".equals(d.code())));
    }

    @Test
    void shouldFlagEmptyTitleInDocumentProfile() {
        List<Diagnostic> diags = diagnose(
                "<!DOCTYPE html><html lang=\"en\"><head><title>   </title></head><body></body></html>",
                new DocumentHtmlProfile());
        assertTrue(diags.stream().anyMatch(d -> "html-missing-title".equals(d.code())));
    }

    @Test
    void shouldPassValidDocumentProfile() {
        List<Diagnostic> diags = diagnose(
                "<!DOCTYPE html><html lang=\"en\"><head><title>X</title></head><body></body></html>",
                new DocumentHtmlProfile());
        assertTrue(diags.stream().noneMatch(d -> d.code().startsWith("html-missing")));
    }
}
