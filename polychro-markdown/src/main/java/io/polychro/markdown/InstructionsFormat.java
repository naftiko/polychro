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

import com.fasterxml.jackson.databind.JsonNode;
import io.polychro.spi.Diagnostic;
import io.polychro.spi.Document;
import io.polychro.spi.Severity;
import io.polychro.spi.SourceRange;

import java.util.List;

/**
 * Format profile for .instructions.md / .prompt.md files — validates frontmatter with applyTo.
 */
class InstructionsFormat implements MarkdownFormat {

    @Override
    public void validate(Document doc, List<Diagnostic> diagnostics) {
        JsonNode data = frontmatter(doc);
        if (data == null) {
            // No frontmatter is acceptable — falls back to generic
            return;
        }

        // applyTo is the key field for instructions files
        if (!data.has("applyTo")) {
            diagnostics.add(new Diagnostic(Severity.WARN, "instructions-missing-applyto",
                    ".instructions.md frontmatter should include 'applyTo' pattern",
                    null, new SourceRange(1, 1, 1, 1)));
        } else {
            JsonNode applyTo = data.get("applyTo");
            if (applyTo.isTextual() && applyTo.asText().isBlank()) {
                diagnostics.add(new Diagnostic(Severity.ERROR, "instructions-empty-applyto",
                        "'applyTo' must not be empty",
                        null, new SourceRange(1, 1, 1, 1)));
            }
        }
    }
}
