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
package io.polychro.html;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

/**
 * Parses raw HTML content using JSoup in either document or fragment mode.
 */
class HtmlParserFacade {

    HtmlParseResult parse(String content, String mode) {
        String normalized = content;
        String effectiveMode = HtmlParseResult.MODE_FRAGMENT.equals(mode)
                ? HtmlParseResult.MODE_FRAGMENT
                : HtmlParseResult.MODE_DOCUMENT;
        Parser parser = Parser.htmlParser().setTrackPosition(true);
        Document document;
        if (HtmlParseResult.MODE_FRAGMENT.equals(effectiveMode)) {
            // Wrap the fragment in a full document so JSoup's tracking parser records
            // accurate positions; calling Parser.parseBodyFragment + document.parser()
            // assigns the parser AFTER parsing, leaving SourceRange un-tracked.
            document = Jsoup.parse("<html><head></head><body>" + normalized + "</body></html>",
                    "", parser);
        } else {
            document = Jsoup.parse(normalized, "", parser);
        }
        return new HtmlParseResult(document, effectiveMode, normalized);
    }
}
