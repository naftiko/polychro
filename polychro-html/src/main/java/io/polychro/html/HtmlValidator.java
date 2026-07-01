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

import io.polychro.spi.Diagnostic;
import io.polychro.spi.Document;
import io.polychro.spi.Validator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Validates an HTML {@link Document}: parses the raw HTML with {@link HtmlParserFacade},
 * projects it via {@link HtmlProjector}, then runs structure / accessibility /
 * security / asset-and-link rules driven by the supplied {@link HtmlProfile}.
 */
class HtmlValidator implements Validator {

    static final String NAME = "html";

    private final HtmlProfile profile;
    private final HtmlParserFacade parserFacade;
    private final HtmlProjector projector;
    private final HtmlStructureChecker structureChecker;
    private final HtmlAccessibilityChecker accessibilityChecker;
    private final HtmlSecurityChecker securityChecker;
    private final HtmlAssetLinkChecker assetLinkChecker;

    HtmlValidator(HtmlProfile profile) {
        this.profile = profile;
        this.parserFacade = new HtmlParserFacade();
        this.projector = new HtmlProjector();
        this.structureChecker = new HtmlStructureChecker();
        this.accessibilityChecker = new HtmlAccessibilityChecker();
        this.securityChecker = new HtmlSecurityChecker();
        this.assetLinkChecker = new HtmlAssetLinkChecker();
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public List<Diagnostic> validate(Document doc) {
        if (doc == null) {
            return Collections.emptyList();
        }
        String content = extractRawContent(doc);
        if (content == null) {
            return Collections.emptyList();
        }
        HtmlParseResult parsed = parserFacade.parse(content, profile.parserMode());
        Document projected = projector.project(parsed, doc.sourcePath());

        List<Diagnostic> diagnostics = new ArrayList<>();
        structureChecker.check(parsed, projected, profile, diagnostics);
        accessibilityChecker.check(parsed, projected, profile, diagnostics);
        securityChecker.check(parsed, projected, profile, diagnostics);
        assetLinkChecker.check(parsed, projected, profile, diagnostics);

        Collections.sort(diagnostics);
        return diagnostics;
    }

    /**
     * Recover the raw HTML source. A {@link io.polychro.spi.DocumentEnricher}-produced document
     * (issue #37) carries a structured, non-textual root but preserves the raw HTML under the
     * {@code "raw.content"} metadata key; prefer that so this parser-based validator keeps working
     * once HTML documents are enriched. Otherwise fall back to the pre-existing raw {@code TextNode}
     * behavior, returning {@code null} for any root that is neither enriched nor textual.
     */
    String extractRawContent(Document doc) {
        Object rawContent = doc.metadata().get("raw.content");
        if (rawContent instanceof String raw) {
            return raw;
        }
        if (doc.root() != null && doc.root().isTextual()) {
            return doc.root().asText();
        }
        return null;
    }
}
