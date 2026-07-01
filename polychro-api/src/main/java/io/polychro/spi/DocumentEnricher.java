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
package io.polychro.spi;

/**
 * Enriches a raw document into a canonical, structurally-projected {@link Document} that
 * ruleset diagnostics (JSONPath {@code given} expressions) can meaningfully traverse.
 *
 * <p>{@code polychro-api} has no dependency on format-specific parsers (commonmark, jsoup, ...),
 * so {@link Document#fromString(String, String, String)} cannot build a structured tree itself
 * for {@code markdown} / {@code html} — it falls back to a raw {@code TextNode} with
 * {@link SourceMap#NONE}. Format modules ({@code polychro-markdown}, {@code polychro-html}) close
 * this gap by registering a {@code DocumentEnricher} via {@link java.util.ServiceLoader}
 * (see {@code META-INF/services/io.polychro.spi.DocumentEnricher}); {@code Document.fromString}
 * discovers and delegates to the matching implementation when present on the classpath.
 *
 * <p>When no enricher is registered for a format, callers keep receiving the previous
 * {@code TextNode} / {@link SourceMap#NONE} document — this is a pure addition, not a breaking
 * change for consumers that only depend on {@code polychro-api}.
 *
 * <p>Implementations must return {@code null} — never throw — for content they cannot enrich, so
 * {@code Document.fromString} can fall back to the raw-text representation gracefully, mirroring
 * the degrade-gracefully contract already used by {@link JacksonSourceMap#forContent}.
 */
public interface DocumentEnricher {

    /**
     * @return the canonical format this enricher handles, e.g. {@code "markdown"} or {@code "html"}
     */
    String format();

    /**
     * Parse and project {@code content} into a canonical {@link Document}.
     *
     * <p>The returned document's {@link Document#root()} should be a structured
     * {@code JsonNode} tree (not a raw {@code TextNode}) so JSONPath {@code given} expressions
     * can traverse it, and its {@link Document#sourceMap()} should resolve projected paths to
     * real {@link SourceRange}s. The original raw content should be preserved under the
     * {@code "raw.content"} key of {@link Document#metadata()} so consumers that need the raw
     * text (e.g. line-based checks) do not need to re-serialize the projected tree.
     *
     * @param content    the raw document content, never {@code null} or blank
     * @param sourcePath the originating path, may be {@code null}
     * @return the enriched document, or {@code null} if this enricher cannot handle the content
     */
    Document enrich(String content, String sourcePath);
}
