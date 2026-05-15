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

import java.util.List;

/**
 * Adapter exposing the anchors and references of a projected {@link Document} to format-agnostic
 * rules.
 *
 * <p>Each format module (e.g. {@code polychro-markdown}, {@code polychro-html}) implements this
 * interface once over its canonical projection shape. The shared rules in
 * {@code polychro-format-common} consume this adapter without depending on any format-specific
 * projection.
 */
public interface DocumentReferenceAdapter {

    /**
     * Whether this adapter can read references from {@code document}.
     *
     * <p>A typical implementation checks the document {@code format} id; rules use this to filter
     * adapters discovered via {@code ServiceLoader} or similar.
     *
     * @param document the document to inspect
     * @return {@code true} when {@link #anchors(Document)} and {@link #references(Document)} can
     *         operate on {@code document}
     */
    boolean supports(Document document);

    /**
     * Collect anchors and ids declared within {@code document}.
     *
     * <p>For Markdown this enumerates heading slugs; for HTML this enumerates element {@code id}
     * attributes and explicit heading ids. Implementations must return a non-null list; ordering
     * matches source order where practical.
     *
     * @param document the projected document
     * @return the collected anchors, never {@code null}
     */
    List<Anchor> anchors(Document document);

    /**
     * Collect link and asset references declared within {@code document}.
     *
     * <p>Implementations parse the raw target via {@link LinkResolver#resolve(String, String)} so
     * the {@link LinkKind} classification is consistent across formats.
     *
     * @param document the projected document
     * @return the collected references, never {@code null}
     */
    List<LinkReference> references(Document document);
}
