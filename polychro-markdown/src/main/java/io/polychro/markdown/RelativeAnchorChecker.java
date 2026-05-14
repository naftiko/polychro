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

    private final MarkdownParserFacade parserFacade = new MarkdownParserFacade(new FrontmatterParser());
    private final MarkdownProjector projector = new MarkdownProjector();

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
            String normalizedAnchor = anchor.toLowerCase();
            var parsed = parserFacade.parse(content);
            var projected = projector.project(parsed, mdFile.toString());
            var headings = projected.root().path("document").path("headings");

            for (int i = 0; i < headings.size(); i++) {
                String projectedAnchor = headings.get(i).path("anchor").asText();
                if (projectedAnchor.equals(normalizedAnchor)) {
                    return true;
                }
            }
        } catch (IOException e) {
            // Can't read file — treat as not found
            return false;
        }
        return false;
    }
}
