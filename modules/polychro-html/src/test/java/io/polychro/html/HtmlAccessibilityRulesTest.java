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

import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlAccessibilityRulesTest {

    private List<Diagnostic> diagnose(String html) {
        Document doc = Document.fromString(html, "html", "/tmp/x.html");
        return new HtmlValidator(new FragmentHtmlProfile()).validate(doc);
    }

    @Test
    void shouldFlagImgWithoutAlt() {
        List<Diagnostic> diags = diagnose("<img src=\"x.png\">");
        assertTrue(diags.stream().anyMatch(d -> "html-img-missing-alt".equals(d.code())));
    }

    @Test
    void shouldNotFlagImgWithAlt() {
        List<Diagnostic> diags = diagnose("<img src=\"x.png\" alt=\"x\">");
        assertTrue(diags.stream().noneMatch(d -> "html-img-missing-alt".equals(d.code())));
    }

    @Test
    void shouldFlagUnlabeledFormField() {
        List<Diagnostic> diags = diagnose(
                "<form><input type=\"text\" name=\"n\"></form>");
        assertTrue(diags.stream().anyMatch(d -> "html-form-field-unlabeled".equals(d.code())));
    }

    @Test
    void shouldNotFlagLabeledFormField() {
        List<Diagnostic> diags = diagnose(
                "<form><label for=\"n\">N</label><input type=\"text\" id=\"n\"></form>");
        assertTrue(diags.stream().noneMatch(d -> "html-form-field-unlabeled".equals(d.code())));
    }

    @Test
    void shouldFlagFieldWithIdButNoLabel() {
        List<Diagnostic> diags = diagnose(
                "<form><input id=\"u\" type=\"text\"></form>");
        assertTrue(diags.stream().anyMatch(d -> "html-form-field-unlabeled".equals(d.code())));
    }

    @Test
    void shouldSkipHiddenSubmitAndButtonFields() {
        List<Diagnostic> diags = diagnose(
                "<form>"
                        + "<input type=\"hidden\" name=\"csrf\">"
                        + "<input type=\"submit\" value=\"go\">"
                        + "<input type=\"button\" value=\"x\">"
                        + "</form>");
        assertTrue(diags.stream().noneMatch(d -> "html-form-field-unlabeled".equals(d.code())));
    }

    @Test
    void shouldFlagTableWithoutHeaders() {
        List<Diagnostic> diags = diagnose(
                "<table><tr><td>x</td></tr></table>");
        assertTrue(diags.stream().anyMatch(d -> "html-table-missing-headers".equals(d.code())));
    }

    @Test
    void shouldNotFlagTableWithHeaders() {
        List<Diagnostic> diags = diagnose(
                "<table><tr><th>A</th></tr><tr><td>x</td></tr></table>");
        assertTrue(diags.stream().noneMatch(d -> "html-table-missing-headers".equals(d.code())));
    }
}
