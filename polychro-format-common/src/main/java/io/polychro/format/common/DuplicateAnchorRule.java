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

import io.polychro.spi.Diagnostic;
import io.polychro.spi.Document;
import io.polychro.spi.Severity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cross-format check: every anchor or id declared in a document must be unique.
 *
 * <p>When the rule is configured with a non-empty companion document list, the same uniqueness
 * check is also applied across the combined set so anchor collisions between sibling files (e.g.
 * a Markdown blueprint and its generated HTML page) can be flagged.
 */
public final class DuplicateAnchorRule {

    public static final String CODE = "duplicate-anchor";

    private final AnchorCollector collector;

    /**
     * Create the rule with an {@link AnchorCollector} backed by one or more format adapters.
     */
    public DuplicateAnchorRule(AnchorCollector collector) {
        if (collector == null) {
            throw new IllegalArgumentException("collector must not be null");
        }
        this.collector = collector;
    }

    /**
     * Apply the rule to a single document, scanning for in-document duplicates only.
     *
     * @param document the projected document
     * @return zero or more diagnostics, severity {@link Severity#WARN}, one per duplicate
     *         occurrence (the first occurrence is treated as the canonical declaration)
     */
    public List<Diagnostic> apply(Document document) {
        return applyAcross(document, List.of());
    }

    /**
     * Apply the rule to {@code document} while also considering anchors declared in
     * {@code companions} as if they shared the same namespace.
     *
     * <p>Companion documents are not themselves the subject of diagnostics; only duplicates that
     * occur within {@code document} (either against itself or against a companion declaration) are
     * reported.
     *
     * @param document   the projected document under test
     * @param companions sibling documents whose anchors share the same namespace
     * @return zero or more diagnostics, severity {@link Severity#WARN}
     */
    public List<Diagnostic> applyAcross(Document document, Iterable<Document> companions) {
        if (document == null) {
            throw new IllegalArgumentException("document must not be null");
        }
        if (companions == null) {
            throw new IllegalArgumentException("companions must not be null");
        }
        Map<String, Integer> seenCount = new LinkedHashMap<>();
        for (Document companion : companions) {
            for (Anchor anchor : collector.collect(companion)) {
                seenCount.merge(anchor.id(), 1, Integer::sum);
            }
        }
        List<Diagnostic> diagnostics = new ArrayList<>();
        for (Anchor anchor : collector.collect(document)) {
            int previous = seenCount.getOrDefault(anchor.id(), 0);
            if (previous >= 1) {
                diagnostics.add(new Diagnostic(Severity.WARN, CODE,
                        "Duplicate anchor '" + anchor.id() + "'",
                        anchor.path(),
                        document.sourceMap().resolve(anchor.path())));
            }
            seenCount.put(anchor.id(), previous + 1);
        }
        return diagnostics;
    }
}
