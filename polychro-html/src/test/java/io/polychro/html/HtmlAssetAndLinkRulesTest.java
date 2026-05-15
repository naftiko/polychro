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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlAssetAndLinkRulesTest {

    private List<Diagnostic> diagnose(String html, String sourcePath) {
        Document doc = Document.fromString(html, "html", sourcePath);
        return new HtmlValidator(new FragmentHtmlProfile()).validate(doc);
    }

    @Test
    void shouldFlagMissingLocalImage(@TempDir Path tempDir) throws IOException {
        Path docFile = tempDir.resolve("page.html");
        Files.writeString(docFile, "x");
        List<Diagnostic> diags = diagnose(
                "<img src=\"missing.png\" alt=\"x\">", docFile.toString());
        assertTrue(diags.stream().anyMatch(d -> "html-missing-local-asset".equals(d.code())));
    }

    @Test
    void shouldResolveExistingLocalImage(@TempDir Path tempDir) throws IOException {
        Path docFile = tempDir.resolve("page.html");
        Files.writeString(docFile, "x");
        Files.writeString(tempDir.resolve("present.png"), "");
        List<Diagnostic> diags = diagnose(
                "<img src=\"present.png\" alt=\"x\">", docFile.toString());
        assertTrue(diags.stream().noneMatch(d -> "html-missing-local-asset".equals(d.code())));
    }

    @Test
    void shouldSkipAbsoluteAndSpecialLinks(@TempDir Path tempDir) throws IOException {
        Path docFile = tempDir.resolve("page.html");
        Files.writeString(docFile, "x");
        List<Diagnostic> diags = diagnose(
                "<a href=\"https://x\">x</a>"
                        + "<a href=\"//x\">x</a>"
                        + "<a href=\"data:foo\">x</a>"
                        + "<a href=\"mailto:a@b\">x</a>"
                        + "<a href=\"javascript:foo()\">x</a>"
                        + "<a href=\"#top\">x</a>",
                docFile.toString());
        assertTrue(diags.stream().noneMatch(d -> "html-missing-local-asset".equals(d.code())));
    }

    @Test
    void shouldStripQueryAndFragmentBeforeResolving(@TempDir Path tempDir) throws IOException {
        Path docFile = tempDir.resolve("page.html");
        Files.writeString(docFile, "x");
        Files.writeString(tempDir.resolve("a.css"), "");
        List<Diagnostic> diags = diagnose(
                "<link rel=\"stylesheet\" href=\"a.css?v=1#top\">", docFile.toString());
        assertTrue(diags.stream().noneMatch(d -> "html-missing-local-asset".equals(d.code())));
    }

    @Test
    void shouldSkipWhenSourcePathIsAbsent() {
        // Document.fromString requires a sourcePath inference. Passing null path skips the check.
        Document doc = Document.fromString("<img src=\"x.png\" alt=\"x\">", "html", null);
        List<Diagnostic> diags = new HtmlValidator(new FragmentHtmlProfile()).validate(doc);
        assertTrue(diags.stream().noneMatch(d -> "html-missing-local-asset".equals(d.code())));
    }

    @Test
    void shouldUseCurrentDirectoryWhenSourceHasNoParent() throws IOException {
        Path localFile = Path.of("page.html");
        Files.writeString(localFile, "x");
        try {
            List<Diagnostic> diags = diagnose(
                    "<img src=\"asset-that-does-not-exist.png\" alt=\"x\">",
                    localFile.toString());
            assertTrue(diags.stream().anyMatch(d -> "html-missing-local-asset".equals(d.code())));
        } finally {
            Files.deleteIfExists(localFile);
        }
    }

    @Test
    void shouldIgnoreEmptyHref(@TempDir Path tempDir) throws IOException {
        Path docFile = tempDir.resolve("page.html");
        Files.writeString(docFile, "x");
        List<Diagnostic> diags = diagnose("<a href=\"\">x</a>", docFile.toString());
        assertTrue(diags.stream().noneMatch(d -> "html-missing-local-asset".equals(d.code())));
    }

    @Test
    void shouldIgnoreUrlThatIsOnlyFragmentOrQuery(@TempDir Path tempDir) throws IOException {
        Path docFile = tempDir.resolve("page.html");
        Files.writeString(docFile, "x");
        // After stripping, "?q" becomes ""
        List<Diagnostic> diags = diagnose("<a href=\"?q\">x</a>", docFile.toString());
        assertTrue(diags.stream().noneMatch(d -> "html-missing-local-asset".equals(d.code())));
    }

    @Test
    void shouldSkipWhenSourcePathIsBlank() {
        Document doc = Document.fromString("<img src=\"x.png\" alt=\"x\">", "html", "  ");
        List<Diagnostic> diags = new HtmlValidator(new FragmentHtmlProfile()).validate(doc);
        assertTrue(diags.stream().noneMatch(d -> "html-missing-local-asset".equals(d.code())));
    }

    @Test
    void shouldSkipHttpAbsoluteLinks(@TempDir Path tempDir) throws IOException {
        Path docFile = tempDir.resolve("page.html");
        Files.writeString(docFile, "x");
        List<Diagnostic> diags = diagnose(
                "<a href=\"http://example.com/x\">x</a>", docFile.toString());
        assertTrue(diags.stream().noneMatch(d -> "html-missing-local-asset".equals(d.code())));
    }
}
