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

/**
 * Strategy contract describing which structural, accessibility and security rules
 * apply for a given HTML profile.
 */
interface HtmlProfile {

    String name();

    /** Whether the profile expects a full top-level document (with html / head / body). */
    boolean requiresDocumentStructure();

    /** Whether {@code <script>} and inline event handlers are permitted. */
    boolean allowsScripts();

    /** Whether inline style attributes are permitted. */
    boolean allowsInlineStyles();

    /** The parser mode this profile maps to (document or fragment). */
    String parserMode();

    static HtmlProfile forName(String profileName) {
        if (profileName == null) {
            return new GenericHtmlProfile();
        }
        return switch (profileName.toLowerCase(java.util.Locale.ROOT)) {
            case "document" -> new DocumentHtmlProfile();
            case "fragment" -> new FragmentHtmlProfile();
            case "email" -> new EmailHtmlProfile();
            case "embedded-ui" -> new EmbeddedUiHtmlProfile();
            default -> new GenericHtmlProfile();
        };
    }
}
