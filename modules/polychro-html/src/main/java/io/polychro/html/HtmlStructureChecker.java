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
import io.polychro.spi.Severity;
import io.polychro.spi.SourceRange;
import org.jsoup.nodes.Element;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Structure rules: duplicate ids, heading order, missing title / lang, broken fragments.
 */
class HtmlStructureChecker {

    void check(HtmlParseResult parsed, Document projected, HtmlProfile profile,
               List<Diagnostic> diagnostics) {
        checkDuplicateIds(parsed, diagnostics);
        checkHeadingOrder(parsed, diagnostics);
        checkBrokenFragments(parsed, diagnostics);
        if (profile.requiresDocumentStructure()) {
            checkRequiredDocumentParts(parsed, diagnostics);
        }
    }

    private void checkDuplicateIds(HtmlParseResult parsed, List<Diagnostic> diagnostics) {
        Set<String> seen = new HashSet<>();
        for (Element el : parsed.document().getAllElements()) {
            String id = el.id();
            if (id.isEmpty()) {
                continue;
            }
            if (!seen.add(id)) {
                diagnostics.add(new Diagnostic(
                        Severity.ERROR,
                        "html-duplicate-id",
                        "Duplicate id '" + id + "' on element <" + el.tagName() + ">",
                        "$.document.nodes",
                        rangeFor(el)));
            }
        }
    }

    private void checkHeadingOrder(HtmlParseResult parsed, List<Diagnostic> diagnostics) {
        int previous = 0;
        int idx = 0;
        for (Element el : parsed.document().select("h1, h2, h3, h4, h5, h6")) {
            int level = Integer.parseInt(el.tagName().substring(1));
            if (previous != 0 && level > previous + 1) {
                diagnostics.add(new Diagnostic(
                        Severity.WARN,
                        "html-heading-order",
                        "Heading level jumps from h" + previous + " to h" + level,
                        "$.document.headings[" + idx + "]",
                        rangeFor(el)));
            }
            previous = level;
            idx++;
        }
    }

    private void checkBrokenFragments(HtmlParseResult parsed, List<Diagnostic> diagnostics) {
        Set<String> ids = new HashSet<>();
        for (Element el : parsed.document().getAllElements()) {
            String id = el.id();
            if (!id.isEmpty()) {
                ids.add(id);
            }
            String nameAttr = el.attr("name");
            if (!nameAttr.isEmpty() && "a".equalsIgnoreCase(el.tagName())) {
                ids.add(nameAttr);
            }
        }
        int idx = 0;
        for (Element a : parsed.document().select("a[href], link[href]")) {
            String href = a.attr("href");
            if (href.startsWith("#") && href.length() > 1) {
                String fragment = href.substring(1);
                if (!ids.contains(fragment)) {
                    diagnostics.add(new Diagnostic(
                            Severity.WARN,
                            "html-broken-fragment",
                            "Anchor link '" + href + "' does not resolve to any element id",
                            "$.document.links[" + idx + "]",
                            rangeFor(a)));
                }
            }
            idx++;
        }
    }

    private void checkRequiredDocumentParts(HtmlParseResult parsed, List<Diagnostic> diagnostics) {
        Element html = parsed.document().selectFirst("html");
        if (html == null) {
            diagnostics.add(new Diagnostic(
                    Severity.WARN,
                    "html-missing-lang",
                    "Document profile requires lang attribute on <html>",
                    "$.document.lang",
                    new SourceRange(1, 1, 1, 1)));
            diagnostics.add(new Diagnostic(
                    Severity.WARN,
                    "html-missing-title",
                    "Document profile requires a non-empty <title>",
                    "$.document.title",
                    new SourceRange(1, 1, 1, 1)));
            return;
        }
        if (html.attr("lang").isEmpty()) {
            diagnostics.add(new Diagnostic(
                    Severity.WARN,
                    "html-missing-lang",
                    "Document profile requires lang attribute on <html>",
                    "$.document.lang",
                    rangeFor(html)));
        }
        Element title = parsed.document().selectFirst("title");
        if (title == null || title.text().trim().isEmpty()) {
            diagnostics.add(new Diagnostic(
                    Severity.WARN,
                    "html-missing-title",
                    "Document profile requires a non-empty <title>",
                    "$.document.title",
                    rangeFor(html)));
        }
    }

    private SourceRange rangeFor(Element el) {
        org.jsoup.nodes.Range range = el.sourceRange();
        if (range.isTracked()) {
            return new SourceRange(range.start().lineNumber(), range.start().columnNumber(),
                    range.end().lineNumber(), range.end().columnNumber());
        }
        return new SourceRange(1, 1, 1, 1);
    }
}
