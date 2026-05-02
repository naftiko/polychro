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
import io.polychro.spi.Severity;
import io.polychro.spi.SourceRange;

import java.util.List;
import java.util.Set;

/**
 * Format profile for AGENTS.md files — validates required sections and no-frontmatter convention.
 */
class AgentsFormat implements MarkdownFormat {

    private static final Set<String> REQUIRED_SECTIONS = Set.of(
            "Build & Test", "Code Style", "Contribution Workflow"
    );

    @Override
    public void validate(Document doc, FrontmatterResult frontmatter, List<Diagnostic> diagnostics) {
        // AGENTS.md should not have frontmatter
        if (frontmatter.hasFrontmatter()) {
            diagnostics.add(new Diagnostic(Severity.WARN, "agents-unexpected-frontmatter",
                    "AGENTS.md should not have YAML frontmatter",
                    null, new SourceRange(1, 1, 1, 1)));
        }

        // Check required sections exist in the body
        String body = frontmatter.body();
        if (body == null) {
            body = "";
        }

        for (String section : REQUIRED_SECTIONS) {
            if (!containsSection(body, section)) {
                diagnostics.add(new Diagnostic(Severity.WARN, "agents-missing-section",
                        "AGENTS.md missing recommended section: ## " + section,
                        null, null));
            }
        }
    }

    boolean containsSection(String body, String sectionName) {
        // Match ## Section Name or ### Section Name etc.
        return body.contains("## " + sectionName)
                || body.contains("# " + sectionName);
    }
}
