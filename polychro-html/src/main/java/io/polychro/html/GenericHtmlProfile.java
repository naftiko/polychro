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

/** Default profile: permissive document validation. */
class GenericHtmlProfile implements HtmlProfile {

    @Override
    public String name() {
        return "generic";
    }

    @Override
    public boolean requiresDocumentStructure() {
        return false;
    }

    @Override
    public boolean allowsScripts() {
        return true;
    }

    @Override
    public boolean allowsInlineStyles() {
        return true;
    }

    @Override
    public String parserMode() {
        return HtmlParseResult.MODE_DOCUMENT;
    }
}
