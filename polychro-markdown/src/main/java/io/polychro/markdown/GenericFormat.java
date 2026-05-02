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
package io.polychro.markdown;

import io.polychro.spi.Diagnostic;
import io.polychro.spi.Document;

import java.util.List;

/**
 * Default generic format — no format-specific rules applied.
 */
class GenericFormat implements MarkdownFormat {

    @Override
    public void validate(Document doc, FrontmatterResult frontmatter, List<Diagnostic> diagnostics) {
        // No format-specific rules for generic Markdown
    }
}
