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

import io.polychro.spi.Diagnostic;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RelativeAnchorCheckerTest {

    private final RelativeAnchorChecker checker = new RelativeAnchorChecker();

    @TempDir
    Path tempDir;

    @Test
    void checkShouldAcceptValidFileAndValidAnchor() throws IOException {
        Files.writeString(tempDir.resolve("target.md"), "# Title\n\n## My Section\n");

        List<MarkdownValidator.LinkInfo> links = List.of(
                new MarkdownValidator.LinkInfo("target.md#my-section", 3));

        List<Diagnostic> diagnostics = checker.check(links, tempDir);
        assertTrue(diagnostics.isEmpty());
    }

    @Test
    void checkShouldReportBrokenAnchorInExistingFile() throws IOException {
        Files.writeString(tempDir.resolve("target.md"), "# Title\n\n## Real Section\n");

        List<MarkdownValidator.LinkInfo> links = List.of(
                new MarkdownValidator.LinkInfo("target.md#nonexistent", 5));

        List<Diagnostic> diagnostics = checker.check(links, tempDir);
        assertEquals(1, diagnostics.size());
        assertEquals("broken-relative-anchor", diagnostics.get(0).code());
        assertTrue(diagnostics.get(0).message().contains("nonexistent"));
        assertTrue(diagnostics.get(0).message().contains("target.md"));
    }

    @Test
    void checkShouldSkipWhenFileDoesNotExist() {
        List<MarkdownValidator.LinkInfo> links = List.of(
                new MarkdownValidator.LinkInfo("missing.md#heading", 1));

        List<Diagnostic> diagnostics = checker.check(links, tempDir);
        assertTrue(diagnostics.isEmpty()); // File check is done by RelativeLinkChecker
    }

    @Test
    void checkShouldSkipNonMarkdownFiles() throws IOException {
        Files.writeString(tempDir.resolve("data.json"), "{\"key\": \"value\"}");

        List<MarkdownValidator.LinkInfo> links = List.of(
                new MarkdownValidator.LinkInfo("data.json#key", 1));

        List<Diagnostic> diagnostics = checker.check(links, tempDir);
        assertTrue(diagnostics.isEmpty());
    }

    @Test
    void checkShouldMatchAnchorCaseInsensitively() throws IOException {
        Files.writeString(tempDir.resolve("target.md"), "# Title\n\n## Build & Test\n");

        List<MarkdownValidator.LinkInfo> links = List.of(
                new MarkdownValidator.LinkInfo("target.md#build--test", 1));

        List<Diagnostic> diagnostics = checker.check(links, tempDir);
        assertTrue(diagnostics.isEmpty());
    }

    @Test
    void checkShouldSkipNonHeadingBlocksWhenMatchingAnchor() throws IOException {
        Files.writeString(tempDir.resolve("target.md"), "Intro paragraph\n\n## Section\n");

        List<MarkdownValidator.LinkInfo> links = List.of(
                new MarkdownValidator.LinkInfo("target.md#section", 1));

        List<Diagnostic> diagnostics = checker.check(links, tempDir);
        assertTrue(diagnostics.isEmpty());
    }

    @Test
    void checkShouldIgnoreLinksWithoutAnchor() throws IOException {
        Files.writeString(tempDir.resolve("target.md"), "# Title\n");

        List<MarkdownValidator.LinkInfo> links = List.of(
                new MarkdownValidator.LinkInfo("target.md", 1));

        List<Diagnostic> diagnostics = checker.check(links, tempDir);
        assertTrue(diagnostics.isEmpty());
    }

    @Test
    void checkShouldIgnoreInternalAnchorOnlyLinks() {
        List<MarkdownValidator.LinkInfo> links = List.of(
                new MarkdownValidator.LinkInfo("#heading", 1));

        List<Diagnostic> diagnostics = checker.check(links, tempDir);
        assertTrue(diagnostics.isEmpty());
    }

    @Test
    void checkShouldIgnoreEmptyAnchor() throws IOException {
        Files.writeString(tempDir.resolve("target.md"), "# Title\n");

        List<MarkdownValidator.LinkInfo> links = List.of(
                new MarkdownValidator.LinkInfo("target.md#", 1));

        List<Diagnostic> diagnostics = checker.check(links, tempDir);
        assertTrue(diagnostics.isEmpty());
    }

    @Test
    void checkShouldHandleMarkdownExtension() throws IOException {
        Files.writeString(tempDir.resolve("file.markdown"), "# Title\n\n## Section\n");

        List<MarkdownValidator.LinkInfo> links = List.of(
                new MarkdownValidator.LinkInfo("file.markdown#section", 1));

        List<Diagnostic> diagnostics = checker.check(links, tempDir);
        assertTrue(diagnostics.isEmpty());
    }

    @Test
    void anchorExistsInFileShouldReturnFalseForUnreadableFile() throws IOException {
        // Create a directory with .md extension — reading it will throw IOException
        Path dir = tempDir.resolve("fake.md");
        Files.createDirectories(dir);

        boolean result = checker.anchorExistsInFile(dir, "heading");
        assertFalse(result);
    }
}
