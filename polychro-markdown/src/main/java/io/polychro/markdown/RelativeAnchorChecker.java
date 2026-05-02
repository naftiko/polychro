package io.polychro.markdown;

import io.polychro.spi.Diagnostic;
import io.polychro.spi.Severity;
import io.polychro.spi.SourceRange;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates relative links that include an anchor fragment (e.g., "other-file.md#heading").
 * Checks both file existence and heading anchor presence in the target Markdown file.
 */
class RelativeAnchorChecker {

    List<Diagnostic> check(List<MarkdownValidator.LinkInfo> links, Path documentDir) {
        List<Diagnostic> diagnostics = new ArrayList<>();

        for (MarkdownValidator.LinkInfo link : links) {
            String href = link.target();
            int hashIndex = href.indexOf('#');
            if (hashIndex < 0) {
                continue; // No anchor — skip
            }

            String pathPart = href.substring(0, hashIndex);
            String anchor = href.substring(hashIndex + 1);

            if (pathPart.isBlank()) {
                continue; // Internal anchor link — handled elsewhere
            }

            if (anchor.isBlank()) {
                continue; // No anchor to validate
            }

            Path targetFile = documentDir.resolve(pathPart).normalize();
            if (!Files.exists(targetFile)) {
                // File doesn't exist — already reported by RelativeLinkChecker, skip anchor check
                continue;
            }

            String fileName = targetFile.getFileName().toString().toLowerCase();
            if (!fileName.endsWith(".md") && !fileName.endsWith(".markdown")) {
                // Not a Markdown file — can't validate anchor
                continue;
            }

            if (!anchorExistsInFile(targetFile, anchor)) {
                diagnostics.add(new Diagnostic(Severity.WARN, "broken-relative-anchor",
                        "Anchor '#" + anchor + "' not found in " + pathPart,
                        null,
                        new SourceRange(link.line(), 1, link.line(), 1)));
            }
        }

        return diagnostics;
    }

    boolean anchorExistsInFile(Path mdFile, String anchor) {
        try {
            String content = Files.readString(mdFile);
            String[] lines = content.split("\n");
            String normalizedAnchor = anchor.toLowerCase();

            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.startsWith("#")) {
                    String headingText = trimmed.replaceFirst("^#+\\s*", "");
                    String slug = MarkdownValidator.slugify(headingText);
                    if (slug.equals(normalizedAnchor)) {
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            // Can't read file — treat as not found
            return false;
        }
        return false;
    }
}
