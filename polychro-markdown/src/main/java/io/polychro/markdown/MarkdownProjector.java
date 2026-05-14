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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.polychro.spi.Document;
import io.polychro.spi.FormatProjector;
import io.polychro.spi.SourceRange;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.BulletList;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.Heading;
import org.commonmark.node.Link;
import org.commonmark.node.ListItem;
import org.commonmark.node.OrderedList;
import org.commonmark.node.Paragraph;

import java.util.Map;

/**
 * Projects parsed Markdown into the canonical document model for rule execution.
 */
class MarkdownProjector implements FormatProjector<MarkdownParseResult> {

    @Override
    public String format() {
        return "markdown";
    }

    @Override
    public Document project(MarkdownParseResult parsed, String sourcePath) {
        ObjectNode root = JsonNodeFactory.instance.objectNode();
        ObjectNode document = root.putObject("document");
        ArrayNode blocks = document.putArray("blocks");
        MarkdownSourceMapBuilder sourceMapBuilder = new MarkdownSourceMapBuilder();

        if (parsed.frontmatter().data() != null) {
            document.set("frontmatter", parsed.frontmatter().data().deepCopy());
            sourceMapBuilder.put("$.document.frontmatter", frontmatterRange(parsed));
        } else {
            document.putNull("frontmatter");
        }

        projectBlocks(parsed, blocks, sourceMapBuilder);

        return new Document(root, format(), sourcePath, sourceMapBuilder.build(), Map.of());
    }

    void projectBlocks(MarkdownParseResult parsed, ArrayNode blocks, MarkdownSourceMapBuilder sourceMapBuilder) {
        appendBlocks(parsed.bodyDocument(), blocks, "$.document.blocks", sourceMapBuilder, parsed.bodyStartLine());
    }

    void appendBlocks(org.commonmark.node.Node parent, ArrayNode blocks, String blocksPath,
                      MarkdownSourceMapBuilder sourceMapBuilder, int bodyStartLine) {
        for (org.commonmark.node.Node child = parent.getFirstChild(); child != null; child = child.getNext()) {
            int index = blocks.size();
            String path = blocksPath + "[" + index + "]";

            appendBlock(child, blocks, path, sourceMapBuilder, bodyStartLine);
        }
    }

    void appendBlock(org.commonmark.node.Node child, ArrayNode blocks, String path,
                     MarkdownSourceMapBuilder sourceMapBuilder, int bodyStartLine) {
        if (child instanceof Heading heading) {
            ObjectNode block = blocks.addObject();
            String text = MarkdownValidator.extractText(heading);
            block.put("type", "heading");
            block.put("level", heading.getLevel());
            block.put("text", text);
            block.put("anchor", MarkdownValidator.slugify(text));
            sourceMapBuilder.put(path, rangeFor(heading, bodyStartLine));
        } else if (child instanceof Paragraph paragraph) {
            ObjectNode block = blocks.addObject();
            block.put("type", "paragraph");
            block.put("text", MarkdownValidator.extractText(paragraph));
            ArrayNode paragraphLinks = block.putArray("links");
            appendParagraphLinks(paragraph, paragraphLinks, path, sourceMapBuilder, bodyStartLine);
            sourceMapBuilder.put(path, rangeFor(paragraph, bodyStartLine));
        } else if (child instanceof BulletList bulletList) {
            ObjectNode block = blocks.addObject();
            block.put("type", "list");
            block.put("ordered", false);
            block.put("marker", String.valueOf(bulletList.getBulletMarker()));
            ArrayNode items = block.putArray("items");
            appendListItems(bulletList, items, path, sourceMapBuilder, bodyStartLine);
            sourceMapBuilder.put(path, rangeFor(bulletList, bodyStartLine));
        } else if (child instanceof OrderedList orderedList) {
            ObjectNode block = blocks.addObject();
            block.put("type", "list");
            block.put("ordered", true);
            block.put("marker", String.valueOf(orderedList.getDelimiter()));
            block.put("startNumber", orderedList.getStartNumber());
            ArrayNode items = block.putArray("items");
            appendListItems(orderedList, items, path, sourceMapBuilder, bodyStartLine);
            sourceMapBuilder.put(path, rangeFor(orderedList, bodyStartLine));
        } else if (child instanceof FencedCodeBlock fencedCodeBlock) {
            ObjectNode block = blocks.addObject();
            block.put("type", "code-block");
            if (fencedCodeBlock.getInfo() == null) {
                block.putNull("language");
            } else {
                block.put("language", fencedCodeBlock.getInfo());
            }
            block.put("content", fencedCodeBlock.getLiteral());
            sourceMapBuilder.put(path, rangeFor(fencedCodeBlock, bodyStartLine));
        }
    }

    void appendListItems(org.commonmark.node.Node listNode, ArrayNode items, String blockPath,
                         MarkdownSourceMapBuilder sourceMapBuilder, int bodyStartLine) {
        for (org.commonmark.node.Node child = listNode.getFirstChild(); child != null; child = child.getNext()) {
            if (child instanceof ListItem listItem) {
                int index = items.size();
                ObjectNode item = items.addObject();
                item.put("text", extractListItemText(listItem));
                ArrayNode itemBlocks = item.putArray("blocks");
                appendBlocks(listItem, itemBlocks, blockPath + ".items[" + index + "].blocks",
                        sourceMapBuilder, bodyStartLine);
            }
        }
    }

    void appendListItemLinks(ListItem listItem, ArrayNode links, String itemPath,
                             MarkdownSourceMapBuilder sourceMapBuilder, int bodyStartLine) {
        for (org.commonmark.node.Node child = listItem.getFirstChild(); child != null; child = child.getNext()) {
            if (child instanceof Paragraph paragraph) {
                appendParagraphLinks(paragraph, links, itemPath, sourceMapBuilder, bodyStartLine);
            }
        }
    }

    String extractListItemText(ListItem listItem) {
        StringBuilder text = new StringBuilder();
        for (org.commonmark.node.Node child = listItem.getFirstChild(); child != null; child = child.getNext()) {
            if (!(child instanceof Paragraph paragraph)) {
                continue;
            }

            if (!text.isEmpty()) {
                text.append('\n');
            }
            text.append(MarkdownValidator.extractText(paragraph));
        }
        return text.toString();
    }

    void appendParagraphLinks(Paragraph paragraph, ArrayNode links, String blockPath,
                              MarkdownSourceMapBuilder sourceMapBuilder, int bodyStartLine) {
        paragraph.accept(new AbstractVisitor() {
            @Override
            public void visit(Link link) {
                String destination = link.getDestination();
                if (destination == null || destination.isBlank()) {
                    visitChildren(link);
                    return;
                }

                int index = links.size();
                links.add(buildProjectedLink(link));
                sourceMapBuilder.put(blockPath + ".links[" + index + "]", rangeFor(link, bodyStartLine));
                visitChildren(link);
            }
        });
    }

    ObjectNode buildProjectedLink(Link link) {
        String destination = link.getDestination();
        ObjectNode projectedLink = JsonNodeFactory.instance.objectNode();
        projectedLink.put("target", destination);
        projectedLink.put("text", MarkdownValidator.extractText(link));
        if (destination.startsWith("#")) {
            projectedLink.put("kind", "internal-anchor");
        } else if (destination.startsWith("http://") || destination.startsWith("https://")) {
            projectedLink.put("kind", "external");
        } else {
            projectedLink.put("kind", "relative");
        }
        return projectedLink;
    }

    private SourceRange frontmatterRange(MarkdownParseResult parsed) {
        int endLine = Math.max(1, parsed.frontmatter().bodyStartLine() - 1);
        return new SourceRange(1, 1, endLine, 1);
    }

    private SourceRange rangeFor(org.commonmark.node.Node node, int bodyStartLine) {
        int line = MarkdownValidator.getNodeLine(node, bodyStartLine);
        return new SourceRange(line, 1, line, 1);
    }
}