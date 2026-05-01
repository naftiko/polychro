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
    public void validate(Document doc, FrontmatterResult frontmatter, List<Diagnostic> diagnostics) {
        if (!frontmatter.hasFrontmatter()) {
            // No frontmatter is acceptable — falls back to generic
            return;
        }

        JsonNode data = frontmatter.data();

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
