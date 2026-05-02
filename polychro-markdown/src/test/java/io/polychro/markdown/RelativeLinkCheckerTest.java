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

class RelativeLinkCheckerTest {

    private final RelativeLinkChecker checker = new RelativeLinkChecker();

    @TempDir
    Path tempDir;

    @Test
    void checkShouldAcceptExistingRelativeFile() throws IOException {
        Files.writeString(tempDir.resolve("target.md"), "# Target\n");

        List<MarkdownValidator.LinkInfo> links = List.of(
                new MarkdownValidator.LinkInfo("target.md", 5));

        List<Diagnostic> diagnostics = checker.check(links, tempDir);
        assertTrue(diagnostics.isEmpty());
    }

    @Test
    void checkShouldReportMissingRelativeFile() {
        List<MarkdownValidator.LinkInfo> links = List.of(
                new MarkdownValidator.LinkInfo("nonexistent.md", 3));

        List<Diagnostic> diagnostics = checker.check(links, tempDir);
        assertEquals(1, diagnostics.size());
        assertEquals("broken-relative-link", diagnostics.get(0).code());
        assertTrue(diagnostics.get(0).message().contains("nonexistent.md"));
    }

    @Test
    void checkShouldResolveNestedPaths() throws IOException {
        Path sub = tempDir.resolve("sub");
        Files.createDirectories(sub);
        Files.writeString(sub.resolve("file.md"), "content");

        List<MarkdownValidator.LinkInfo> links = List.of(
                new MarkdownValidator.LinkInfo("sub/file.md", 2));

        List<Diagnostic> diagnostics = checker.check(links, tempDir);
        assertTrue(diagnostics.isEmpty());
    }

    @Test
    void checkShouldResolveParentDirectoryPaths() throws IOException {
        Path sub = tempDir.resolve("docs");
        Files.createDirectories(sub);
        Files.writeString(tempDir.resolve("root.md"), "content");

        List<MarkdownValidator.LinkInfo> links = List.of(
                new MarkdownValidator.LinkInfo("../root.md", 1));

        List<Diagnostic> diagnostics = checker.check(links, sub);
        assertTrue(diagnostics.isEmpty());
    }

    @Test
    void checkShouldAcceptLinkToDirectory() throws IOException {
        Path sub = tempDir.resolve("folder");
        Files.createDirectories(sub);

        List<MarkdownValidator.LinkInfo> links = List.of(
                new MarkdownValidator.LinkInfo("folder", 1));

        List<Diagnostic> diagnostics = checker.check(links, tempDir);
        assertTrue(diagnostics.isEmpty());
    }

    @Test
    void checkShouldHandleUrlEncodedSpaces() throws IOException {
        Files.writeString(tempDir.resolve("my file.md"), "content");

        List<MarkdownValidator.LinkInfo> links = List.of(
                new MarkdownValidator.LinkInfo("my%20file.md", 1));

        List<Diagnostic> diagnostics = checker.check(links, tempDir);
        assertTrue(diagnostics.isEmpty());
    }

    @Test
    void checkShouldRejectAbsolutePath() {
        List<MarkdownValidator.LinkInfo> links = List.of(
                new MarkdownValidator.LinkInfo("/absolute/path.md", 4));

        List<Diagnostic> diagnostics = checker.check(links, tempDir);
        assertEquals(1, diagnostics.size());
        assertEquals("broken-relative-link", diagnostics.get(0).code());
        assertTrue(diagnostics.get(0).message().contains("Absolute path"));
    }

    @Test
    void checkShouldReportEmptyHref() {
        List<MarkdownValidator.LinkInfo> links = List.of(
                new MarkdownValidator.LinkInfo("", 2));

        List<Diagnostic> diagnostics = checker.check(links, tempDir);
        assertEquals(1, diagnostics.size());
        assertEquals("broken-relative-link", diagnostics.get(0).code());
        assertTrue(diagnostics.get(0).message().contains("Empty"));
    }

    @Test
    void checkShouldIgnoreAnchorOnlyLinks() {
        List<MarkdownValidator.LinkInfo> links = List.of(
                new MarkdownValidator.LinkInfo("#heading", 1));

        List<Diagnostic> diagnostics = checker.check(links, tempDir);
        assertTrue(diagnostics.isEmpty());
    }

    @Test
    void checkShouldStripAnchorForFileExistenceCheck() throws IOException {
        Files.writeString(tempDir.resolve("readme.md"), "# Title\n");

        List<MarkdownValidator.LinkInfo> links = List.of(
                new MarkdownValidator.LinkInfo("readme.md#title", 1));

        List<Diagnostic> diagnostics = checker.check(links, tempDir);
        assertTrue(diagnostics.isEmpty());
    }

    @Test
    void checkShouldReportMissingFileWithAnchor() {
        List<MarkdownValidator.LinkInfo> links = List.of(
                new MarkdownValidator.LinkInfo("missing.md#section", 1));

        List<Diagnostic> diagnostics = checker.check(links, tempDir);
        assertEquals(1, diagnostics.size());
        assertEquals("broken-relative-link", diagnostics.get(0).code());
    }

    @Test
    void checkShouldHandleNullTarget() {
        List<MarkdownValidator.LinkInfo> links = List.of(
                new MarkdownValidator.LinkInfo(null, 1));

        List<Diagnostic> diagnostics = checker.check(links, tempDir);
        assertEquals(1, diagnostics.size());
        assertEquals("broken-relative-link", diagnostics.get(0).code());
    }

    @Test
    void checkShouldHandleInvalidPathCharacters() {
        // NUL character creates an InvalidPathException
        List<MarkdownValidator.LinkInfo> links = List.of(
                new MarkdownValidator.LinkInfo("file\u0000name.md", 1));

        List<Diagnostic> diagnostics = checker.check(links, tempDir);
        assertEquals(1, diagnostics.size());
        assertEquals("broken-relative-link", diagnostics.get(0).code());
        assertTrue(diagnostics.get(0).message().contains("Invalid path"));
    }

    @Test
    void checkShouldHandleBadPercentEncoding() throws IOException {
        // Badly encoded URL (incomplete percent-encoding)
        Files.writeString(tempDir.resolve("file.md"), "content");

        List<MarkdownValidator.LinkInfo> links = List.of(
                new MarkdownValidator.LinkInfo("file%2.md", 1));

        List<Diagnostic> diagnostics = checker.check(links, tempDir);
        // Should still work - falls back to raw path
        // The file "file%2.md" doesn't exist, so it reports broken
        assertEquals(1, diagnostics.size());
    }
}
