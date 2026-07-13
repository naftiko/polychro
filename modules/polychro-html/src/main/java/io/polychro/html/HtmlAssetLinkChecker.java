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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Asset and link rules: missing local href/src targets.
 *
 * External link probing is intentionally not performed here; it remains an opt-in
 * extension to keep validation deterministic and offline by default.
 */
class HtmlAssetLinkChecker {

    void check(HtmlParseResult parsed, Document projected, HtmlProfile profile,
               List<Diagnostic> diagnostics) {
        checkMissingLocalAssets(parsed, projected, diagnostics);
    }

    private void checkMissingLocalAssets(HtmlParseResult parsed, Document projected,
                                         List<Diagnostic> diagnostics) {
        Path basePath = resolveBasePath(projected);
        if (basePath == null) {
            return;
        }
        for (Element el : parsed.document().select("a[href], img[src], link[href], script[src]")) {
            String url = el.hasAttr("href") ? el.attr("href") : el.attr("src");
            if (url.isEmpty() || isAbsoluteOrSpecial(url)) {
                continue;
            }
            String pathPart = stripFragmentAndQuery(url);
            if (pathPart.isEmpty()) {
                continue;
            }
            Path resolved = basePath.resolve(pathPart).normalize();
            if (!Files.exists(resolved)) {
                diagnostics.add(new Diagnostic(
                        Severity.WARN,
                        "html-missing-local-asset",
                        "Local asset '" + url + "' does not exist",
                        "$.document.nodes",
                        rangeFor(el)));
            }
        }
    }

    private Path resolveBasePath(Document projected) {
        String sourcePath = projected.sourcePath();
        if (sourcePath == null || sourcePath.isBlank()) {
            return null;
        }
        Path path = Path.of(sourcePath);
        Path parent = path.getParent();
        return parent == null ? Path.of(".") : parent;
    }

    private boolean isAbsoluteOrSpecial(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        return lower.startsWith("http://") || lower.startsWith("https://")
                || lower.startsWith("//") || lower.startsWith("data:")
                || lower.startsWith("mailto:") || lower.startsWith("javascript:")
                || lower.startsWith("#");
    }

    private String stripFragmentAndQuery(String url) {
        int hash = url.indexOf('#');
        String result = hash >= 0 ? url.substring(0, hash) : url;
        int query = result.indexOf('?');
        return query >= 0 ? result.substring(0, query) : result;
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
