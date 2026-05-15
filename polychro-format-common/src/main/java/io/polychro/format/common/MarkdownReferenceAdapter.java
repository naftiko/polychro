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

import com.fasterxml.jackson.databind.JsonNode;
import io.polychro.spi.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link DocumentReferenceAdapter} for the canonical Markdown projection shape emitted by
 * {@code polychro-markdown}.
 *
 * <p>Reads anchors from heading blocks under {@code $.document.blocks[*]} where {@code type} is
 * {@code "heading"} and {@code anchor} is set. Reads references from {@code $.document.links[*]}
 * using the {@code target} field.
 */
public final class MarkdownReferenceAdapter implements DocumentReferenceAdapter {

    private static final String FORMAT = "markdown";

    @Override
    public boolean supports(Document document) {
        return document != null && FORMAT.equals(document.format())
                && document.root() != null && document.root().isObject();
    }

    @Override
    public List<Anchor> anchors(Document document) {
        List<Anchor> anchors = new ArrayList<>();
        JsonNode blocks = document.root().path("document").path("blocks");
        if (blocks.isArray()) {
            for (int i = 0; i < blocks.size(); i++) {
                JsonNode block = blocks.get(i);
                if ("heading".equals(block.path("type").asText(null))) {
                    String anchor = block.path("anchor").asText(null);
                    if (anchor != null && !anchor.isEmpty()) {
                        anchors.add(new Anchor(anchor,
                                "$.document.blocks[" + i + "].anchor",
                                "heading"));
                    }
                }
            }
        }
        return anchors;
    }

    @Override
    public List<LinkReference> references(Document document) {
        List<LinkReference> references = new ArrayList<>();
        JsonNode links = document.root().path("document").path("links");
        if (links.isArray()) {
            for (int i = 0; i < links.size(); i++) {
                JsonNode link = links.get(i);
                String target = link.path("target").asText(null);
                references.add(LinkResolver.resolve(target,
                        "$.document.links[" + i + "].target"));
            }
        }
        return references;
    }
}
