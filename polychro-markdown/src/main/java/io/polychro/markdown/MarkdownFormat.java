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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.StreamSupport;

/**
 * Format-specific validation profile for Markdown documents.
 */
interface MarkdownFormat {

    /**
     * Apply format-specific validation rules.
     */
    void validate(Document doc, List<Diagnostic> diagnostics);

    default JsonNode frontmatter(Document doc) {
        if (doc == null || doc.root() == null) {
            return null;
        }

        JsonNode frontmatter = doc.root().path("document").path("frontmatter");
        if (frontmatter.isMissingNode() || frontmatter.isNull()) {
            return null;
        }
        return frontmatter;
    }

    default Iterable<JsonNode> headings(Document doc) {
        if (doc == null) {
            return Collections.emptyList();
        }
        if (doc.root() == null) {
            return Collections.emptyList();
        }

        JsonNode blocks = doc.root().path("document").path("blocks");
        if (blocks.isArray()) {
            List<JsonNode> headings = new ArrayList<>();
            for (JsonNode block : blocks) {
                if ("heading".equals(block.path("type").asText())) {
                    headings.add(block);
                }
            }
            return headings;
        }

        JsonNode headings = doc.root().path("document").path("headings");
        if (!headings.isArray()) {
            return Collections.emptyList();
        }
        return headings;
    }

    default boolean hasHeading(Document doc, String text, int minLevel) {
        return StreamSupport.stream(headings(doc).spliterator(), false)
                .anyMatch(heading -> heading.path("level").asInt() >= minLevel
                        && text.equals(heading.path("text").asText()));
    }

    default boolean hasHeadingAtOrAboveLevel(Document doc, int minLevel) {
        return StreamSupport.stream(headings(doc).spliterator(), false)
                .anyMatch(heading -> heading.path("level").asInt() >= minLevel);
    }
}
