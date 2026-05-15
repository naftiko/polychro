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
 * {@link DocumentReferenceAdapter} for the canonical HTML projection shape emitted by
 * {@code polychro-html}.
 *
 * <p>Anchors are collected from explicit element ids (walked recursively over
 * {@code $.document.nodes}) — the heading {@code id} attributes surface via the same walk because
 * the HTML projector emits headings inside {@code nodes} with an {@code id} attribute when
 * present.
 *
 * <p>References are collected from {@code $.document.links[*]} using the {@code href} field.
 */
public final class HtmlReferenceAdapter implements DocumentReferenceAdapter {

    private static final String FORMAT = "html";

    @Override
    public boolean supports(Document document) {
        return document != null && FORMAT.equals(document.format())
                && document.root() != null && document.root().isObject();
    }

    @Override
    public List<Anchor> anchors(Document document) {
        List<Anchor> anchors = new ArrayList<>();
        JsonNode nodes = document.root().path("document").path("nodes");
        if (nodes.isArray()) {
            walk(nodes, "$.document.nodes", anchors);
        }
        return anchors;
    }

    private void walk(JsonNode array, String path, List<Anchor> anchors) {
        for (int i = 0; i < array.size(); i++) {
            JsonNode node = array.get(i);
            String nodePath = path + "[" + i + "]";
            String id = node.path("id").asText(null);
            if (id != null && !id.isEmpty()) {
                anchors.add(new Anchor(id, nodePath + ".id", "element"));
            }
            JsonNode children = node.path("children");
            if (children.isArray()) {
                walk(children, nodePath + ".children", anchors);
            }
        }
    }

    @Override
    public List<LinkReference> references(Document document) {
        List<LinkReference> references = new ArrayList<>();
        JsonNode links = document.root().path("document").path("links");
        if (links.isArray()) {
            for (int i = 0; i < links.size(); i++) {
                JsonNode link = links.get(i);
                String href = link.path("href").asText(null);
                references.add(LinkResolver.resolve(href,
                        "$.document.links[" + i + "].href"));
            }
        }
        return references;
    }
}
