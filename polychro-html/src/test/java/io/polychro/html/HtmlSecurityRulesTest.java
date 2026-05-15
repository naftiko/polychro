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

class HtmlSecurityRulesTest {

    private List<Diagnostic> diagnose(String html, HtmlProfile profile) {
        Document doc = Document.fromString(html, "html", "/tmp/x.html");
        return new HtmlValidator(profile).validate(doc);
    }

    @Test
    void shouldFlagInlineEventHandler() {
        List<Diagnostic> diags = diagnose(
                "<button onclick=\"foo()\">x</button>", new FragmentHtmlProfile());
        assertTrue(diags.stream().anyMatch(d -> "html-inline-event-handler".equals(d.code())));
    }

    @Test
    void shouldFlagJavascriptUrlOnAnchor() {
        List<Diagnostic> diags = diagnose(
                "<a href=\"javascript:foo()\">x</a>", new FragmentHtmlProfile());
        assertTrue(diags.stream().anyMatch(d -> "html-javascript-url".equals(d.code())));
    }

    @Test
    void shouldFlagJavascriptUrlOnIframe() {
        List<Diagnostic> diags = diagnose(
                "<iframe src=\"javascript:foo()\"></iframe>", new FragmentHtmlProfile());
        assertTrue(diags.stream().anyMatch(d -> "html-javascript-url".equals(d.code())));
    }

    @Test
    void shouldFlagJavascriptUrlOnFormAction() {
        List<Diagnostic> diags = diagnose(
                "<form action=\"javascript:foo()\"></form>", new FragmentHtmlProfile());
        assertTrue(diags.stream().anyMatch(d -> "html-javascript-url".equals(d.code())));
    }

    @Test
    void shouldFlagTargetBlankWithoutNoopener() {
        List<Diagnostic> diags = diagnose(
                "<a href=\"https://x.com\" target=\"_blank\">x</a>", new FragmentHtmlProfile());
        assertTrue(diags.stream().anyMatch(d -> "html-target-blank-noopener".equals(d.code())));
    }

    @Test
    void shouldNotFlagTargetBlankWithNoopener() {
        List<Diagnostic> diags = diagnose(
                "<a href=\"https://x.com\" target=\"_blank\" rel=\"noopener\">x</a>",
                new FragmentHtmlProfile());
        assertTrue(diags.stream().noneMatch(d -> "html-target-blank-noopener".equals(d.code())));
    }

    @Test
    void shouldNotFlagTargetBlankWithNoreferrer() {
        List<Diagnostic> diags = diagnose(
                "<a href=\"https://x.com\" target=\"_blank\" rel=\"noreferrer\">x</a>",
                new FragmentHtmlProfile());
        assertTrue(diags.stream().noneMatch(d -> "html-target-blank-noopener".equals(d.code())));
    }

    @Test
    void shouldFlagScriptInEmailProfile() {
        List<Diagnostic> diags = diagnose(
                "<script>alert(1)</script>", new EmailHtmlProfile());
        assertTrue(diags.stream().anyMatch(d -> "html-script-disallowed".equals(d.code())));
    }

    @Test
    void shouldFlagInlineStyleInEmbeddedUiProfile() {
        List<Diagnostic> diags = diagnose(
                "<div style=\"color:red\">x</div>", new EmbeddedUiHtmlProfile());
        assertTrue(diags.stream().anyMatch(d -> "html-inline-style-disallowed".equals(d.code())));
    }
}
