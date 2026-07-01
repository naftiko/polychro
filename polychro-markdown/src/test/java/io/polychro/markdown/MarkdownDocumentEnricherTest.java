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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.polychro.spi.Document;
import io.polychro.spi.DocumentEnricher;
import io.polychro.spi.SourceRange;
import java.util.ServiceLoader;
import org.junit.jupiter.api.Test;

class MarkdownDocumentEnricherTest {

    private final MarkdownDocumentEnricher enricher = new MarkdownDocumentEnricher();

    @Test
    void formatShouldBeMarkdown() {
        assertEquals("markdown", enricher.format());
    }

    @Test
    void enrichShouldProduceStructuredRootTraversableByJsonPath() {
        String content = "# Title\n\n## Section\n";
        Document enriched = enricher.enrich(content, "test.md");

        assertNotNull(enriched);
        assertFalse(enriched.root().isTextual(),
                "Enriched root must be a structured tree, not a raw TextNode");
        assertTrue(enriched.root().path("document").path("blocks").isArray(),
                "Structured root must expose $.document.blocks for JSONPath traversal");
    }

    @Test
    void enrichShouldPreserveRawContentInMetadata() {
        String content = "# Title\n\nBody text.\n";
        Document enriched = enricher.enrich(content, "test.md");

        assertEquals(content, enriched.metadata().get("raw.content"));
    }

    @Test
    void enrichShouldResolveSourceRangeForProjectedPath() {
        String content = "# Title\n\n## Section\n";
        Document enriched = enricher.enrich(content, "test.md");

        SourceRange range = enriched.sourceMap().resolve("$.document.blocks[0]");
        assertNotNull(range, "Expected a resolved SourceRange for the first heading block");
    }

    @Test
    void enrichShouldReturnNullForBlankContent() {
        assertNull(enricher.enrich("", "test.md"));
        assertNull(enricher.enrich("   \n\t", "test.md"));
        assertNull(enricher.enrich(null, "test.md"));
    }

    @Test
    void enricherShouldBeDiscoverableViaServiceLoader() {
        boolean found = false;
        for (DocumentEnricher discovered : ServiceLoader.load(DocumentEnricher.class)) {
            if ("markdown".equals(discovered.format())
                    && discovered instanceof MarkdownDocumentEnricher) {
                found = true;
            }
        }
        assertTrue(found, "MarkdownDocumentEnricher must be registered via META-INF/services");
    }
}
