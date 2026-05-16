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

import com.fasterxml.jackson.databind.JsonNode;
import io.polychro.spi.Diagnostic;
import io.polychro.spi.Document;
import org.junit.jupiter.api.Test;
import org.jsoup.Jsoup;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers fallback branches that are not exercised by the standard parser-driven flow:
 * <ul>
 *   <li>{@link HtmlProjector#extractLang} returning {@code null} when the JSoup
 *       document has no {@code <html>} element.</li>
 *   <li>{@code rangeFor} and {@code precisionFor} fallbacks (in {@link HtmlProjector}
 *       and each {@code Html*Checker}) when {@code Range#isTracked()} is {@code false}
 *       &mdash; the default state of any JSoup document parsed without a
 *       position-tracking parser.</li>
 * </ul>
 * These are correctness safety nets that the {@link HtmlParserFacade} no longer
 * triggers (it always configures position tracking before parsing), so they need
 * a dedicated test to keep the 100% line / branch coverage gate green.
 */
class HtmlCoverageEdgeCasesTest {

    private final HtmlProjector projector = new HtmlProjector();

    @Test
    void extractLangShouldReturnNullWhenNoHtmlElement() {
        // Build a JSoup Document and strip the implicit <html> wrapper so
        // selectFirst("html") returns null in HtmlProjector#extractLang.
        org.jsoup.nodes.Document jsoupDoc = Jsoup.parse("<p>x</p>");
        jsoupDoc.children().remove();
        assertNotNull(jsoupDoc);
        // Sanity check: there really is no <html> element anymore.
        assertTrue(jsoupDoc.selectFirst("html") == null);

        HtmlParseResult parsed = new HtmlParseResult(
                jsoupDoc, HtmlParseResult.MODE_DOCUMENT, "<p>x</p>");
        Document projected = projector.project(parsed, null);
        JsonNode docNode = projected.root().get("document");
        assertTrue(docNode.get("lang").isNull(),
                "lang should be null when no <html> element is present");
    }

    @Test
    void projectorAndCheckersShouldFallBackWhenSourceRangesAreUntracked() {
        // Jsoup.parse(...) without a configured tracking parser leaves every
        // Range with isTracked() == false, exercising the fallback in:
        //   - HtmlProjector#rangeFor   (returns SourceRange(1,1,1,1))
        //   - HtmlProjector#precisionFor (returns PRECISION_APPROXIMATE)
        //   - each HtmlAccessibility/Security/Structure/AssetLink checker's
        //     own rangeFor fallback.
        String html = """
                <!DOCTYPE html>
                <html>
                <head></head>
                <body>
                  <h1 id="dup">A</h1>
                  <h3 id="dup">jumps from h1 to h3 + duplicate id</h3>
                  <a href="#missing">broken fragment</a>
                  <a href="javascript:foo()" target="_blank">bad</a>
                  <img src="missing.png">
                  <form action="/x" method="post">
                    <input type="text" name="n">
                  </form>
                  <table><tr><td>no header</td></tr></table>
                  <p style="color:red">inline style</p>
                  <script>console.log(1)</script>
                </body>
                </html>
                """;
        org.jsoup.nodes.Document jsoupDoc = Jsoup.parse(html, "");
        HtmlParseResult parsed = new HtmlParseResult(
                jsoupDoc, HtmlParseResult.MODE_DOCUMENT, html);

        // Project: exercises HtmlProjector#rangeFor & precisionFor fallbacks.
        Document projected = projector.project(parsed, "/tmp/untracked.html");
        assertNotNull(projected);

        // Use the strictest profile to maximise checker coverage (no scripts,
        // no inline styles, document structure required).
        HtmlProfile profile = new DocumentHtmlProfile();
        List<Diagnostic> diagnostics = new ArrayList<>();
        new HtmlStructureChecker().check(parsed, projected, profile, diagnostics);
        new HtmlAccessibilityChecker().check(parsed, projected, profile, diagnostics);
        new HtmlSecurityChecker().check(parsed, projected, profile, diagnostics);
        new HtmlAssetLinkChecker().check(parsed, projected, profile, diagnostics);

        // Each checker should have produced at least one diagnostic, proving
        // its rangeFor() path was taken with an untracked Range.
        assertFalse(diagnostics.isEmpty());
        assertTrue(diagnostics.stream().anyMatch(d -> d.code().startsWith("html-duplicate-id")
                || d.code().startsWith("html-heading-order")
                || d.code().startsWith("html-broken-fragment")
                || d.code().startsWith("html-missing-lang")
                || d.code().startsWith("html-missing-title")));
        assertTrue(diagnostics.stream().anyMatch(d -> d.code().equals("html-img-missing-alt")
                || d.code().equals("html-form-field-unlabeled")
                || d.code().equals("html-table-missing-headers")));
        assertTrue(diagnostics.stream().anyMatch(d -> d.code().equals("html-javascript-url")
                || d.code().equals("html-target-blank-noopener")
                || d.code().equals("html-script-disallowed")
                || d.code().equals("html-inline-style-disallowed")));
        assertTrue(diagnostics.stream().anyMatch(d -> d.code().equals("html-missing-local-asset")));
    }
}
