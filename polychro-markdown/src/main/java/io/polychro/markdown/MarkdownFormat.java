package io.polychro.markdown;

import io.polychro.spi.Diagnostic;
import io.polychro.spi.Document;

import java.util.List;

/**
 * Format-specific validation profile for Markdown documents.
 */
interface MarkdownFormat {

    /**
     * Apply format-specific validation rules.
     */
    void validate(Document doc, FrontmatterResult frontmatter, List<Diagnostic> diagnostics);
}
