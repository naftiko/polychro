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
 * Format profile for SKILL.md files — validates frontmatter structure and body requirements.
 */
class SkillFormat implements MarkdownFormat {

    private static final List<String> REQUIRED_FRONTMATTER_FIELDS = List.of("name", "description");

    @Override
    public void validate(Document doc, FrontmatterResult frontmatter, List<Diagnostic> diagnostics) {
        if (!frontmatter.hasFrontmatter()) {
            diagnostics.add(new Diagnostic(Severity.ERROR, "skill-missing-frontmatter",
                    "SKILL.md requires YAML frontmatter with 'name' and 'description'",
                    null, new SourceRange(1, 1, 1, 1)));
            return;
        }

        JsonNode data = frontmatter.data();
        for (String field : REQUIRED_FRONTMATTER_FIELDS) {
            if (!data.has(field) || data.get(field).isNull()) {
                diagnostics.add(new Diagnostic(Severity.ERROR, "skill-missing-field",
                        "SKILL.md frontmatter missing required field: " + field,
                        null, new SourceRange(1, 1, 1, 1)));
            } else if (data.get(field).isTextual() && data.get(field).asText().isBlank()) {
                diagnostics.add(new Diagnostic(Severity.ERROR, "skill-empty-field",
                        "SKILL.md frontmatter field must not be empty: " + field,
                        null, new SourceRange(1, 1, 1, 1)));
            }
        }

        // Check version field — must be quoted string if present
        if (data.has("version") && !data.get("version").isTextual()) {
            diagnostics.add(new Diagnostic(Severity.WARN, "skill-version-not-string",
                    "SKILL.md 'version' should be a quoted string (e.g. \"1.0\"), found: "
                            + data.get("version").getNodeType(),
                    null, new SourceRange(1, 1, 1, 1)));
        }

        // Body must have at least one ## section
        String body = frontmatter.body();
        if (body != null && !body.contains("\n## ") && !body.startsWith("## ")) {
            diagnostics.add(new Diagnostic(Severity.WARN, "skill-no-sections",
                    "SKILL.md body should have at least one ## section",
                    null, null));
        }
    }
}
