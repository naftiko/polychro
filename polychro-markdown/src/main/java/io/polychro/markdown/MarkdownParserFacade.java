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

import org.commonmark.node.Node;
import org.commonmark.parser.IncludeSourceSpans;
import org.commonmark.parser.Parser;

/**
 * Parses raw Markdown content into frontmatter and CommonMark body structures.
 */
class MarkdownParserFacade {

    private static final Parser PARSER = Parser.builder()
            .includeSourceSpans(IncludeSourceSpans.BLOCKS_AND_INLINES)
            .build();

    private final FrontmatterParser frontmatterParser;

    MarkdownParserFacade(FrontmatterParser frontmatterParser) {
        this.frontmatterParser = frontmatterParser;
    }

    MarkdownParseResult parse(String content) {
        FrontmatterResult frontmatter = frontmatterParser.parse(content);
        Node bodyDocument = PARSER.parse(frontmatter.body());
        return new MarkdownParseResult(content, frontmatter, bodyDocument);
    }
}
