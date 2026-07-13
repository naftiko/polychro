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

import io.polychro.spi.Document;
import io.polychro.spi.DocumentEnricher;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link DocumentEnricher} for {@code markdown}: parses raw content with
 * {@link MarkdownParserFacade} and projects it into the canonical document model via
 * {@link MarkdownProjector}, so ruleset diagnostics (JSONPath {@code given} expressions such as
 * {@code $.document.blocks[*]}) can traverse real structure and carry a resolved
 * {@link io.polychro.spi.SourceRange} instead of matching nothing against a raw {@code TextNode}.
 *
 * <p>Registered via {@code META-INF/services/io.polychro.spi.DocumentEnricher}; discovered and
 * invoked by {@link Document#fromString(String, String, String)} whenever this module is on the
 * classpath. The raw markdown is preserved under the {@code "raw.content"} metadata key so
 * line-based checks (e.g. {@link MarkdownValidator}) do not need to re-serialize the projected
 * tree.
 */
public class MarkdownDocumentEnricher implements DocumentEnricher {

    private final MarkdownParserFacade parserFacade =
            new MarkdownParserFacade(new FrontmatterParser());
    private final MarkdownProjector projector = new MarkdownProjector();

    @Override
    public String format() {
        return "markdown";
    }

    @Override
    public Document enrich(String content, String sourcePath) {
        if (content == null || content.isBlank()) {
            return null;
        }
        MarkdownParseResult parsed = parserFacade.parse(content);
        Document projected = projector.project(parsed, sourcePath);

        // Preserve the raw markdown alongside the projector's own metadata so line-based
        // checks can recover it without re-serializing the projected tree.
        Map<String, Object> metadata = new HashMap<>(projected.metadata());
        metadata.put("raw.content", content);

        return new Document(projected.root(), projected.format(), projected.sourcePath(),
                projected.sourceMap(), metadata);
    }
}
