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

import java.util.List;

/**
 * Accessibility rules: alt text, form labels, table headers.
 */
class HtmlAccessibilityChecker {

    void check(HtmlParseResult parsed, Document projected, HtmlProfile profile,
               List<Diagnostic> diagnostics) {
        checkImgAlt(parsed, diagnostics);
        checkFormLabels(parsed, diagnostics);
        checkTableHeaders(parsed, diagnostics);
    }

    private void checkImgAlt(HtmlParseResult parsed, List<Diagnostic> diagnostics) {
        for (Element img : parsed.document().select("img")) {
            if (!img.hasAttr("alt")) {
                diagnostics.add(new Diagnostic(
                        Severity.WARN,
                        "html-img-missing-alt",
                        "<img> element is missing alt attribute",
                        "$.document.nodes",
                        rangeFor(img)));
            }
        }
    }

    private void checkFormLabels(HtmlParseResult parsed, List<Diagnostic> diagnostics) {
        int formIdx = 0;
        for (Element form : parsed.document().select("form")) {
            int fieldIdx = 0;
            for (Element field : form.select("input, select, textarea")) {
                String type = field.attr("type");
                if ("hidden".equalsIgnoreCase(type) || "submit".equalsIgnoreCase(type)
                        || "button".equalsIgnoreCase(type)) {
                    fieldIdx++;
                    continue;
                }
                String id = field.id();
                boolean labeled = !id.isEmpty()
                        && !parsed.document().select("label[for=" + id + "]").isEmpty();
                if (!labeled) {
                    diagnostics.add(new Diagnostic(
                            Severity.WARN,
                            "html-form-field-unlabeled",
                            "Form field <" + field.tagName() + "> has no associated <label>",
                            "$.document.forms[" + formIdx + "].fields[" + fieldIdx + "]",
                            rangeFor(field)));
                }
                fieldIdx++;
            }
            formIdx++;
        }
    }

    private void checkTableHeaders(HtmlParseResult parsed, List<Diagnostic> diagnostics) {
        int idx = 0;
        for (Element table : parsed.document().select("table")) {
            boolean hasHeader = !table.select("th").isEmpty();
            if (!hasHeader) {
                diagnostics.add(new Diagnostic(
                        Severity.WARN,
                        "html-table-missing-headers",
                        "<table> has no header cells (<th>)",
                        "$.document.tables[" + idx + "].headers",
                        rangeFor(table)));
            }
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
