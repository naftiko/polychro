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

import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.polychro.spi.Diagnostic;
import io.polychro.spi.Document;
import io.polychro.spi.Validator;
import io.polychro.spi.ValidatorConfig;
import io.polychro.spi.ValidatorFactory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlValidatorIntegrationTest {

    @Test
    void factoryShouldBeDiscoverableViaServiceLoader() {
        Optional<ValidatorFactory> found = ServiceLoader.load(ValidatorFactory.class).stream()
                .map(ServiceLoader.Provider::get)
                .filter(f -> "html".equals(f.name()))
                .findFirst();
        assertTrue(found.isPresent());
        assertEquals("html", found.get().name());
        assertTrue(found.get().supportedFormats().contains("html"));
        assertTrue(found.get().supportedProfiles().contains("document"));
    }

    @Test
    void factoryShouldCreateValidatorWithProfileFromConfig() {
        ValidatorFactory factory = new HtmlValidatorFactory();
        Validator validator = factory.create(new ValidatorConfig(Map.of("profile", "email")));
        assertNotNull(validator);
        assertEquals("html", validator.name());
    }

    @Test
    void factoryShouldDefaultProfileToGenericWhenMissing() {
        ValidatorFactory factory = new HtmlValidatorFactory();
        Validator validator = factory.create(new ValidatorConfig(Map.of()));
        assertNotNull(validator);
    }

    @Test
    void validatorShouldReturnEmptyForNullOrNonTextualRoot() {
        HtmlValidator validator = new HtmlValidator(new FragmentHtmlProfile());
        assertEquals(List.of(), validator.validate(null));
        Document numeric = new Document(IntNode.valueOf(1), "html", "/tmp/x.html");
        assertEquals(List.of(), validator.validate(numeric));
    }

    @Test
    void validatorShouldReturnEmptyForNullRootWithoutRawContent() {
        // Neither a textual root nor a "raw.content" metadata key: nothing to validate.
        Document nullRoot = new Document(null, "html", "/tmp/x.html");
        assertEquals(List.of(), new HtmlValidator(new FragmentHtmlProfile()).validate(nullRoot));
    }

    @Test
    void validatorShouldFallBackToTextualRootWhenNoRawContentMetadata() {
        // Pre-enricher behavior: a raw TextNode root (no metadata) is still validated.
        String html = "<!DOCTYPE html><html lang=\"en\"><head><title>t</title></head>"
                + "<body><h1>x</h1><h3>y</h3></body></html>";
        Document textual = new Document(TextNode.valueOf(html), "html", "/tmp/x.html");
        List<Diagnostic> diagnostics =
                new HtmlValidator(new DocumentHtmlProfile()).validate(textual);
        assertTrue(diagnostics.stream().anyMatch(d -> "html-heading-order".equals(d.code())),
                "Expected html-heading-order from textual root but got: " + diagnostics);
    }

    @Test
    void validatorShouldReadRawContentFromMetadataWhenRootIsStructured() {
        // A DocumentEnricher-produced document has a structured (non-textual) root but preserves
        // the raw HTML under the "raw.content" metadata key. The validator must recover the raw
        // HTML from metadata rather than short-circuiting to an empty diagnostic list as it does
        // for a bare non-textual root.
        String html = "<!DOCTYPE html><html lang=\"en\"><head><title>t</title></head>"
                + "<body><h1>x</h1><h3>y</h3></body></html>";
        Document enriched = new Document(IntNode.valueOf(1), "html", "/tmp/x.html", null,
                Map.of("raw.content", html));

        List<Diagnostic> diagnostics = new HtmlValidator(new DocumentHtmlProfile()).validate(enriched);

        assertTrue(diagnostics.stream().anyMatch(d -> "html-heading-order".equals(d.code())),
                "Expected html-heading-order from raw.content metadata but got: " + diagnostics);
    }

    @Test
    void validatorShouldReturnSortedDiagnostics() {
        String html = """
                <!DOCTYPE html><html><body>
                  <h1>x</h1><h3>y</h3>
                  <button onclick="x">b</button>
                  <a href="javascript:x()">x</a>
                  <a href="https://x" target="_blank">x</a>
                  <img src="x.png">
                  <div id="a"></div><div id="a"></div>
                </body></html>
                """;
        Document doc = Document.fromString(html, "html", "/tmp/x.html");
        List<Diagnostic> diagnostics = new HtmlValidator(new DocumentHtmlProfile()).validate(doc);
        assertTrue(diagnostics.size() > 1);
        // sorted by severity ascending ordinal (ERROR < WARN), then by path
        for (int i = 1; i < diagnostics.size(); i++) {
            int prev = diagnostics.get(i - 1).severity().ordinal();
            int curr = diagnostics.get(i).severity().ordinal();
            assertTrue(prev <= curr);
        }
    }
}
