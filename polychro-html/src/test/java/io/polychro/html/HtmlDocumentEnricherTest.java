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
package io.polychro.html;

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

class HtmlDocumentEnricherTest {

    private final HtmlDocumentEnricher enricher = new HtmlDocumentEnricher();

    @Test
    void formatShouldBeHtml() {
        assertEquals("html", enricher.format());
    }

    @Test
    void enrichShouldProduceStructuredRootTraversableByJsonPath() {
        String html = "<!DOCTYPE html><html lang=\"en\"><head><title>t</title></head>"
                + "<body><h1>x</h1></body></html>";
        Document enriched = enricher.enrich(html, "test.html");

        assertNotNull(enriched);
        assertFalse(enriched.root().isTextual(),
                "Enriched root must be a structured tree, not a raw TextNode");
        assertTrue(enriched.root().path("document").path("nodes").isArray(),
                "Structured root must expose $.document.nodes for JSONPath traversal");
    }

    @Test
    void enrichShouldPreserveRawContentInMetadata() {
        String html = "<!DOCTYPE html><html lang=\"en\"><head><title>t</title></head>"
                + "<body><h1>x</h1></body></html>";
        Document enriched = enricher.enrich(html, "test.html");

        assertEquals(html, enriched.metadata().get("raw.content"));
    }

    @Test
    void enrichShouldResolveSourceRangeForProjectedPath() {
        String html = "<!DOCTYPE html><html lang=\"en\"><head><title>t</title></head>"
                + "<body><h1>x</h1></body></html>";
        Document enriched = enricher.enrich(html, "test.html");

        SourceRange range = enriched.sourceMap().resolve("$.document.nodes[0]");
        assertNotNull(range, "Expected a resolved SourceRange for the first projected node");
    }

    @Test
    void enrichShouldReturnNullForBlankContent() {
        assertNull(enricher.enrich("", "test.html"));
        assertNull(enricher.enrich("   \n\t", "test.html"));
        assertNull(enricher.enrich(null, "test.html"));
    }

    @Test
    void enricherShouldBeDiscoverableViaServiceLoader() {
        boolean found = false;
        for (DocumentEnricher discovered : ServiceLoader.load(DocumentEnricher.class)) {
            if ("html".equals(discovered.format())
                    && discovered instanceof HtmlDocumentEnricher) {
                found = true;
            }
        }
        assertTrue(found, "HtmlDocumentEnricher must be registered via META-INF/services");
    }
}
