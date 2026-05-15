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
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;

import java.util.List;
import java.util.Locale;

/**
 * Security rules: inline event handlers, javascript: URLs, target=_blank without rel=noopener,
 * inline scripts and styles per profile.
 */
class HtmlSecurityChecker {

    void check(HtmlParseResult parsed, Document projected, HtmlProfile profile,
               List<Diagnostic> diagnostics) {
        checkInlineEventHandlers(parsed, diagnostics);
        checkJavascriptUrls(parsed, diagnostics);
        checkTargetBlankNoopener(parsed, diagnostics);
        if (!profile.allowsScripts()) {
            checkScriptsDisallowed(parsed, diagnostics);
        }
        if (!profile.allowsInlineStyles()) {
            checkInlineStylesDisallowed(parsed, diagnostics);
        }
    }

    private void checkInlineEventHandlers(HtmlParseResult parsed, List<Diagnostic> diagnostics) {
        int idx = 0;
        for (Element el : parsed.document().getAllElements()) {
            for (Attribute attr : el.attributes()) {
                String key = attr.getKey().toLowerCase(Locale.ROOT);
                if (key.startsWith("on")) {
                    diagnostics.add(new Diagnostic(
                            Severity.ERROR,
                            "html-inline-event-handler",
                            "Inline event handler '" + attr.getKey() + "' is not allowed",
                            "$.document.nodes[" + idx + "].attributes." + attr.getKey(),
                            rangeFor(el)));
                }
            }
            idx++;
        }
    }

    private void checkJavascriptUrls(HtmlParseResult parsed, List<Diagnostic> diagnostics) {
        int idx = 0;
        for (Element el : parsed.document().select("a[href], area[href], iframe[src], form[action]")) {
            String url = el.hasAttr("href") ? el.attr("href")
                    : el.hasAttr("src") ? el.attr("src") : el.attr("action");
            if (url.toLowerCase(Locale.ROOT).startsWith("javascript:")) {
                diagnostics.add(new Diagnostic(
                        Severity.ERROR,
                        "html-javascript-url",
                        "javascript: URL is not allowed on <" + el.tagName() + ">",
                        "$.document.links[" + idx + "]",
                        rangeFor(el)));
            }
            idx++;
        }
    }

    private void checkTargetBlankNoopener(HtmlParseResult parsed, List<Diagnostic> diagnostics) {
        int idx = 0;
        for (Element a : parsed.document().select("a[target=_blank], area[target=_blank]")) {
            String rel = a.attr("rel").toLowerCase(Locale.ROOT);
            if (!rel.contains("noopener") && !rel.contains("noreferrer")) {
                diagnostics.add(new Diagnostic(
                        Severity.WARN,
                        "html-target-blank-noopener",
                        "target=\"_blank\" requires rel=\"noopener\" (or noreferrer)",
                        "$.document.links[" + idx + "].rel",
                        rangeFor(a)));
            }
            idx++;
        }
    }

    private void checkScriptsDisallowed(HtmlParseResult parsed, List<Diagnostic> diagnostics) {
        int idx = 0;
        for (Element script : parsed.document().select("script")) {
            diagnostics.add(new Diagnostic(
                    Severity.ERROR,
                    "html-script-disallowed",
                    "<script> is not allowed by the current profile",
                    "$.document.scripts[" + idx + "]",
                    rangeFor(script)));
            idx++;
        }
    }

    private void checkInlineStylesDisallowed(HtmlParseResult parsed, List<Diagnostic> diagnostics) {
        int idx = 0;
        for (Element el : parsed.document().select("[style]")) {
            diagnostics.add(new Diagnostic(
                    Severity.WARN,
                    "html-inline-style-disallowed",
                    "Inline style attribute is not allowed by the current profile",
                    "$.document.nodes[" + idx + "].attributes.style",
                    rangeFor(el)));
            idx++;
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
