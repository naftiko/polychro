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
 * <p>The projection nests both anchors and link references under the recursive
 * {@code blocks[*]} / {@code blocks[*].items[*].blocks[*]} tree:
 * <ul>
 *   <li>Heading anchors live at {@code $.document.blocks[i].anchor} (and recursively inside
 *       list items at {@code $.document.blocks[i].items[j].blocks[k].anchor}).</li>
 *   <li>Link references live at {@code $.document.blocks[i].links[j].target} (and recursively
 *       inside list items at {@code $.document.blocks[i].items[j].blocks[k].links[l].target},
 *       plus {@code $.document.blocks[i].items[j].links[l].target} when the projector emits
 *       links directly on a list item).</li>
 * </ul>
 *
 * <p>This adapter mirrors {@code MarkdownValidator.collectProjectedHeadings} and
 * {@code MarkdownValidator.collectProjectedBlockLinks}.
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
        collectAnchors(blocks, "$.document.blocks", anchors);
        return anchors;
    }

    @Override
    public List<LinkReference> references(Document document) {
        List<LinkReference> references = new ArrayList<>();
        JsonNode blocks = document.root().path("document").path("blocks");
        collectReferences(blocks, "$.document.blocks", references);
        return references;
    }

    private void collectAnchors(JsonNode blocks, String blocksPath, List<Anchor> out) {
        if (!blocks.isArray()) {
            return;
        }
        for (int i = 0; i < blocks.size(); i++) {
            JsonNode block = blocks.get(i);
            String blockPath = blocksPath + "[" + i + "]";
            if ("heading".equals(block.path("type").asText(null))) {
                String anchor = block.path("anchor").asText(null);
                if (anchor != null && !anchor.isEmpty()) {
                    out.add(new Anchor(anchor, blockPath + ".anchor", "heading"));
                }
            }

            JsonNode items = block.path("items");
            if (!items.isArray()) {
                continue;
            }
            for (int j = 0; j < items.size(); j++) {
                JsonNode nestedBlocks = items.get(j).path("blocks");
                collectAnchors(nestedBlocks, blockPath + ".items[" + j + "].blocks", out);
            }
        }
    }

    private void collectReferences(JsonNode blocks, String blocksPath, List<LinkReference> out) {
        if (!blocks.isArray()) {
            return;
        }
        for (int i = 0; i < blocks.size(); i++) {
            JsonNode block = blocks.get(i);
            String blockPath = blocksPath + "[" + i + "]";
            collectLinkArray(block.path("links"), blockPath + ".links", out);

            JsonNode items = block.path("items");
            if (!items.isArray()) {
                continue;
            }
            for (int j = 0; j < items.size(); j++) {
                JsonNode item = items.get(j);
                String itemPath = blockPath + ".items[" + j + "]";
                collectLinkArray(item.path("links"), itemPath + ".links", out);
                JsonNode nestedBlocks = item.path("blocks");
                collectReferences(nestedBlocks, itemPath + ".blocks", out);
            }
        }
    }

    private void collectLinkArray(JsonNode links, String pathPrefix, List<LinkReference> out) {
        if (!links.isArray()) {
            return;
        }
        for (int i = 0; i < links.size(); i++) {
            JsonNode link = links.get(i);
            String target = link.path("target").asText(null);
            out.add(LinkResolver.resolve(target, pathPrefix + "[" + i + "].target"));
        }
    }
}
