package io.polychro.markdown;

import io.polychro.spi.Diagnostic;
import io.polychro.spi.Severity;
import io.polychro.spi.SourceRange;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates relative file links in Markdown documents.
 * Resolves paths relative to the document's directory and checks file existence.
 */
class RelativeLinkChecker {

    List<Diagnostic> check(List<MarkdownValidator.LinkInfo> links, Path documentDir) {
        List<Diagnostic> diagnostics = new ArrayList<>();

        for (MarkdownValidator.LinkInfo link : links) {
            String href = link.target();
            if (href == null || href.isBlank()) {
                diagnostics.add(new Diagnostic(Severity.WARN, "broken-relative-link",
                        "Empty relative link href",
                        null,
                        new SourceRange(link.line(), 1, link.line(), 1)));
                continue;
            }

            // Strip anchor fragment for file existence check
            String pathPart = href.contains("#") ? href.substring(0, href.indexOf('#')) : href;

            if (pathPart.isBlank()) {
                // It's an anchor-only link like "#heading" — skip (handled by internal link checker)
                continue;
            }

            // Reject absolute paths
            if (pathPart.startsWith("/")) {
                diagnostics.add(new Diagnostic(Severity.WARN, "broken-relative-link",
                        "Absolute path not allowed in relative link: " + pathPart,
                        null,
                        new SourceRange(link.line(), 1, link.line(), 1)));
                continue;
            }

            // Decode URL-encoded characters
            String decoded;
            try {
                decoded = URLDecoder.decode(pathPart, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                decoded = pathPart;
            }

            // Resolve the path
            try {
                Path resolved = documentDir.resolve(decoded).normalize();
                if (!Files.exists(resolved)) {
                    diagnostics.add(new Diagnostic(Severity.WARN, "broken-relative-link",
                            "Relative link target not found: " + pathPart,
                            null,
                            new SourceRange(link.line(), 1, link.line(), 1)));
                }
            } catch (InvalidPathException e) {
                diagnostics.add(new Diagnostic(Severity.WARN, "broken-relative-link",
                        "Invalid path in relative link: " + pathPart,
                        null,
                        new SourceRange(link.line(), 1, link.line(), 1)));
            }
        }

        return diagnostics;
    }
}
