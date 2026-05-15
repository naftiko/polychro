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
package io.polychro.format.common;

import io.polychro.spi.Document;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Aggregates anchors across one or more projected documents.
 *
 * <p>The collector is format-agnostic: it consumes any {@link DocumentReferenceAdapter} that knows
 * how to read anchors from a {@link Document}. Returned indexes preserve insertion order so
 * duplicate-detection rules can deterministically report the second-and-subsequent occurrences.
 */
public final class AnchorCollector {

    private final List<DocumentReferenceAdapter> adapters;

    /**
     * Create a collector backed by the given adapters.
     *
     * @param adapters ordered list of format adapters; the first adapter that {@link
     *                 DocumentReferenceAdapter#supports(Document)} a document is used
     */
    public AnchorCollector(List<DocumentReferenceAdapter> adapters) {
        if (adapters == null) {
            throw new IllegalArgumentException("adapters must not be null");
        }
        this.adapters = List.copyOf(adapters);
    }

    /**
     * Collect anchors from {@code document}.
     *
     * @param document the projected document, never {@code null}
     * @return the anchors declared in {@code document}; empty when no adapter supports the
     *         document
     */
    public List<Anchor> collect(Document document) {
        if (document == null) {
            throw new IllegalArgumentException("document must not be null");
        }
        for (DocumentReferenceAdapter adapter : adapters) {
            if (adapter.supports(document)) {
                return adapter.anchors(document);
            }
        }
        return List.of();
    }

    /**
     * Collect anchors from multiple documents and group them by id.
     *
     * @param documents the documents to scan
     * @return a map keyed by anchor id; entries with size {@code > 1} indicate duplicates
     */
    public Map<String, List<Anchor>> collectByIdAcross(Iterable<Document> documents) {
        if (documents == null) {
            throw new IllegalArgumentException("documents must not be null");
        }
        Map<String, List<Anchor>> byId = new LinkedHashMap<>();
        for (Document document : documents) {
            for (Anchor anchor : collect(document)) {
                byId.computeIfAbsent(anchor.id(), key -> new ArrayList<>()).add(anchor);
            }
        }
        return byId;
    }

    /**
     * Build the set of distinct anchor ids declared in {@code document}.
     *
     * @param document the projected document, never {@code null}
     * @return the distinct ids; never {@code null}
     */
    public Set<String> idsOf(Document document) {
        List<Anchor> collected = collect(document);
        Set<String> ids = new java.util.LinkedHashSet<>();
        for (Anchor anchor : collected) {
            ids.add(anchor.id());
        }
        return ids;
    }
}
