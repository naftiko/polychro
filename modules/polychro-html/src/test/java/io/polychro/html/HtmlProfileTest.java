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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlProfileTest {

    @Test
    void forNameShouldReturnGenericForNullOrUnknown() {
        assertInstanceOf(GenericHtmlProfile.class, HtmlProfile.forName(null));
        assertInstanceOf(GenericHtmlProfile.class, HtmlProfile.forName("nope"));
    }

    @Test
    void forNameShouldMapKnownProfiles() {
        assertInstanceOf(DocumentHtmlProfile.class, HtmlProfile.forName("document"));
        assertInstanceOf(FragmentHtmlProfile.class, HtmlProfile.forName("fragment"));
        assertInstanceOf(EmailHtmlProfile.class, HtmlProfile.forName("email"));
        assertInstanceOf(EmbeddedUiHtmlProfile.class, HtmlProfile.forName("embedded-ui"));
        assertInstanceOf(GenericHtmlProfile.class, HtmlProfile.forName("generic"));
    }

    @Test
    void genericProfileShouldExposeExpectedDefaults() {
        HtmlProfile p = new GenericHtmlProfile();
        assertEquals("generic", p.name());
        assertFalse(p.requiresDocumentStructure());
        assertTrue(p.allowsScripts());
        assertTrue(p.allowsInlineStyles());
        assertEquals(HtmlParseResult.MODE_DOCUMENT, p.parserMode());
    }

    @Test
    void documentProfileShouldRequireStructure() {
        HtmlProfile p = new DocumentHtmlProfile();
        assertEquals("document", p.name());
        assertTrue(p.requiresDocumentStructure());
        assertTrue(p.allowsScripts());
        assertTrue(p.allowsInlineStyles());
        assertEquals(HtmlParseResult.MODE_DOCUMENT, p.parserMode());
    }

    @Test
    void fragmentProfileShouldUseFragmentMode() {
        HtmlProfile p = new FragmentHtmlProfile();
        assertEquals("fragment", p.name());
        assertFalse(p.requiresDocumentStructure());
        assertTrue(p.allowsScripts());
        assertTrue(p.allowsInlineStyles());
        assertEquals(HtmlParseResult.MODE_FRAGMENT, p.parserMode());
    }

    @Test
    void emailProfileShouldDisallowScripts() {
        HtmlProfile p = new EmailHtmlProfile();
        assertEquals("email", p.name());
        assertFalse(p.requiresDocumentStructure());
        assertFalse(p.allowsScripts());
        assertTrue(p.allowsInlineStyles());
        assertEquals(HtmlParseResult.MODE_DOCUMENT, p.parserMode());
    }

    @Test
    void embeddedUiProfileShouldDisallowScriptsAndStyles() {
        HtmlProfile p = new EmbeddedUiHtmlProfile();
        assertEquals("embedded-ui", p.name());
        assertFalse(p.requiresDocumentStructure());
        assertFalse(p.allowsScripts());
        assertFalse(p.allowsInlineStyles());
        assertEquals(HtmlParseResult.MODE_FRAGMENT, p.parserMode());
    }
}
