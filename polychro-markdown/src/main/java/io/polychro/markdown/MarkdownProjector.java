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
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.Heading;
import org.commonmark.node.Link;

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
        ArrayNode headings = document.putArray("headings");
        ArrayNode links = document.putArray("links");
        ArrayNode codeBlocks = document.putArray("codeBlocks");
        MarkdownSourceMapBuilder sourceMapBuilder = new MarkdownSourceMapBuilder();

        if (parsed.frontmatter().data() != null) {
            document.set("frontmatter", parsed.frontmatter().data().deepCopy());
            sourceMapBuilder.put("$.document.frontmatter", frontmatterRange(parsed));
        } else {
            document.putNull("frontmatter");
        }

        parsed.bodyDocument().accept(new AbstractVisitor() {
            @Override
            public void visit(Heading heading) {
                int index = headings.size();
                String path = "$.document.headings[" + index + "]";
                ObjectNode projectedHeading = headings.addObject();
                String text = MarkdownValidator.extractText(heading);
                projectedHeading.put("level", heading.getLevel());
                projectedHeading.put("text", text);
                projectedHeading.put("anchor", MarkdownValidator.slugify(text));
                sourceMapBuilder.put(path, rangeFor(heading, parsed.bodyStartLine()));
            }

            @Override
            public void visit(Link link) {
                String destination = link.getDestination();
                if (destination == null || destination.isBlank()) {
                    visitChildren(link);
                    return;
                }

                int index = links.size();
                String path = "$.document.links[" + index + "]";
                ObjectNode projectedLink = links.addObject();
                projectedLink.put("target", destination);
                projectedLink.put("text", MarkdownValidator.extractText(link));
                if (destination.startsWith("#")) {
                    projectedLink.put("kind", "internal-anchor");
                } else if (destination.startsWith("http://") || destination.startsWith("https://")) {
                    projectedLink.put("kind", "external");
                } else {
                    projectedLink.put("kind", "relative");
                }
                sourceMapBuilder.put(path, rangeFor(link, parsed.bodyStartLine()));
                visitChildren(link);
            }

            @Override
            public void visit(FencedCodeBlock fencedCodeBlock) {
                int index = codeBlocks.size();
                String path = "$.document.codeBlocks[" + index + "]";
                ObjectNode projectedCodeBlock = codeBlocks.addObject();
                if (fencedCodeBlock.getInfo() == null) {
                    projectedCodeBlock.putNull("language");
                } else {
                    projectedCodeBlock.put("language", fencedCodeBlock.getInfo());
                }
                projectedCodeBlock.put("content", fencedCodeBlock.getLiteral());
                sourceMapBuilder.put(path, rangeFor(fencedCodeBlock, parsed.bodyStartLine()));
            }
        });

        return new Document(root, format(), sourcePath, sourceMapBuilder.build(), Map.of());
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