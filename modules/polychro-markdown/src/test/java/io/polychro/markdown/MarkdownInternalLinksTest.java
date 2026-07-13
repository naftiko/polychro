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

import com.fasterxml.jackson.databind.node.TextNode;
import io.polychro.spi.Diagnostic;
import io.polychro.spi.Document;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownInternalLinksTest {

    private final MarkdownValidator validator = new MarkdownValidator(
            120, "-", new GenericFormat(), new FrontmatterParser());

    private Document doc(String content) {
        return new Document(new TextNode(content), "test.md");
    }

    @Test
    void validateShouldAcceptLinkMatchingHeadingSlug() {
        String content = "# Title\n\n## My Section\n\n[link](#my-section)\n";
        List<Diagnostic> result = validator.validate(doc(content));
        assertTrue(result.stream().noneMatch(d -> d.code().equals("broken-internal-link")));
    }

    @Test
    void validateShouldReportLinkNotMatchingAnyHeading() {
        String content = "# Title\n\n## Section\n\n[link](#nonexistent)\n";
        List<Diagnostic> result = validator.validate(doc(content));
        assertTrue(result.stream().anyMatch(d -> d.code().equals("broken-internal-link")));
    }

    @Test
    void validateShouldHandleSpecialCharactersInSlug() {
        String content = "# Title\n\n## Build & Test\n\n[link](#build--test)\n";
        List<Diagnostic> result = validator.validate(doc(content));
        assertTrue(result.stream().noneMatch(d -> d.code().equals("broken-internal-link")));
    }

    @Test
    void validateShouldMatchCaseInsensitively() {
        String content = "# Title\n\n## My Section\n\n[link](#My-Section)\n";
        List<Diagnostic> result = validator.validate(doc(content));
        assertTrue(result.stream().noneMatch(d -> d.code().equals("broken-internal-link")));
    }

    @Test
    void validateShouldReportBrokenLinkWithSpecialChars() {
        String content = "# Title\n\n## Section\n\n[link](#section-that-does-not-exist)\n";
        List<Diagnostic> result = validator.validate(doc(content));
        assertTrue(result.stream().anyMatch(d -> d.code().equals("broken-internal-link")));
    }
}
