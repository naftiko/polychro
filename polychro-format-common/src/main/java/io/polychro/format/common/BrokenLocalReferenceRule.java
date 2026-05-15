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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Cross-format check: every relative-file and internal-anchor reference must resolve.
 *
 * <p>For {@link LinkKind#INTERNAL_ANCHOR} references the rule verifies that the fragment matches
 * an anchor declared in the same document.
 *
 * <p>For {@link LinkKind#RELATIVE_FILE} references the rule verifies that the on-disk path exists
 * relative to the document's {@code sourcePath}. The file-existence check is injected to keep
 * the rule deterministic and testable.
 */
public final class BrokenLocalReferenceRule {

    public static final String CODE = "broken-local-reference";

    private final DocumentReferenceAdapter adapter;
    private final Predicate<Path> fileExists;

    /**
     * Create the rule with a default file-existence predicate backed by {@link Files#exists}.
     *
     * @param adapter the format adapter that exposes anchors and references for the document
     */
    public BrokenLocalReferenceRule(DocumentReferenceAdapter adapter) {
        this(adapter, Files::exists);
    }

    /**
     * Create the rule with an explicit file-existence predicate.
     *
     * @param adapter    the format adapter that exposes anchors and references for the document
     * @param fileExists predicate used to test for relative-file targets
     */
    public BrokenLocalReferenceRule(DocumentReferenceAdapter adapter, Predicate<Path> fileExists) {
        if (adapter == null) {
            throw new IllegalArgumentException("adapter must not be null");
        }
        if (fileExists == null) {
            throw new IllegalArgumentException("fileExists must not be null");
        }
        this.adapter = adapter;
        this.fileExists = fileExists;
    }

    /**
     * Apply the rule to {@code document}.
     *
     * @param document the projected document
     * @return zero or more diagnostics, severity {@link Severity#WARN}
     */
    public List<Diagnostic> apply(Document document) {
        if (document == null) {
            throw new IllegalArgumentException("document must not be null");
        }
        if (!adapter.supports(document)) {
            return List.of();
        }
        Set<String> anchorIds = new LinkedHashSet<>();
        for (Anchor anchor : adapter.anchors(document)) {
            anchorIds.add(anchor.id());
        }
        Path documentPath = document.sourcePath() == null ? null : Path.of(document.sourcePath());
        Path documentDir = documentPath == null ? null
                : (documentPath.getParent() == null ? Path.of(".") : documentPath.getParent());

        List<Diagnostic> diagnostics = new ArrayList<>();
        for (LinkReference reference : adapter.references(document)) {
            if (reference.kind() == LinkKind.INTERNAL_ANCHOR) {
                if (reference.fragment() == null || !anchorIds.contains(reference.fragment())) {
                    diagnostics.add(new Diagnostic(Severity.WARN, CODE,
                            "Anchor '#" + (reference.fragment() == null ? "" : reference.fragment())
                                    + "' is not declared in the document",
                            reference.path(),
                            document.sourceMap().resolve(reference.path())));
                }
            } else if (reference.kind() == LinkKind.RELATIVE_FILE) {
                if (documentDir == null) {
                    continue;
                }
                Path resolved = documentDir.resolve(reference.filePart()).normalize();
                if (!fileExists.test(resolved)) {
                    diagnostics.add(new Diagnostic(Severity.WARN, CODE,
                            "Relative reference '" + reference.target() + "' does not exist on disk",
                            reference.path(),
                            document.sourceMap().resolve(reference.path())));
                }
            }
        }
        return diagnostics;
    }
}
