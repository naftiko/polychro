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
