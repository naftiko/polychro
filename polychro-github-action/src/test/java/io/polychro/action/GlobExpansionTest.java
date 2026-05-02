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
package io.polychro.action;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GlobExpansionTest {

    @TempDir
    Path tempDir;

    @Test
    void expandShouldMatchSingleGlob() throws IOException {
        Files.writeString(tempDir.resolve("cap.yml"), "test: true");
        Files.writeString(tempDir.resolve("other.json"), "{}");

        List<Path> results = GlobExpansion.expand(tempDir, "*.yml");
        assertEquals(1, results.size());
        assertTrue(results.get(0).getFileName().toString().equals("cap.yml"));
    }

    @Test
    void expandShouldMatchMultipleGlobsCommaSeparated() throws IOException {
        Files.writeString(tempDir.resolve("a.yml"), "a");
        Files.writeString(tempDir.resolve("b.json"), "b");
        Files.writeString(tempDir.resolve("c.txt"), "c");

        List<Path> results = GlobExpansion.expand(tempDir, "*.yml,*.json");
        assertEquals(2, results.size());
    }

    @Test
    void expandShouldMatchRecursiveGlob() throws IOException {
        Path sub = tempDir.resolve("capabilities");
        Files.createDirectories(sub);
        Files.writeString(sub.resolve("api.yml"), "api");
        Files.writeString(tempDir.resolve("root.yml"), "root");

        List<Path> results = GlobExpansion.expand(tempDir, "**/*.yml,*.yml");
        assertEquals(2, results.size());
    }

    @Test
    void expandShouldReturnEmptyForNoMatches() throws IOException {
        Files.writeString(tempDir.resolve("data.txt"), "txt");

        List<Path> results = GlobExpansion.expand(tempDir, "*.yml");
        assertTrue(results.isEmpty());
    }

    @Test
    void expandShouldReturnEmptyForNullPattern() {
        List<Path> results = GlobExpansion.expand(tempDir, null);
        assertTrue(results.isEmpty());
    }

    @Test
    void expandShouldReturnEmptyForBlankPattern() {
        List<Path> results = GlobExpansion.expand(tempDir, "   ");
        assertTrue(results.isEmpty());
    }

    @Test
    void expandShouldDeduplicateResults() throws IOException {
        Files.writeString(tempDir.resolve("dup.yml"), "dup");

        List<Path> results = GlobExpansion.expand(tempDir, "*.yml,*.yml");
        assertEquals(1, results.size());
    }

    @Test
    void expandShouldHandleNewlineSeparatedPatterns() throws IOException {
        Files.writeString(tempDir.resolve("a.yml"), "a");
        Files.writeString(tempDir.resolve("b.json"), "b");

        List<Path> results = GlobExpansion.expand(tempDir, "*.yml\n*.json");
        assertEquals(2, results.size());
    }

    @Test
    void expandShouldSkipEmptyPatternParts() throws IOException {
        Files.writeString(tempDir.resolve("a.yml"), "a");

        List<Path> results = GlobExpansion.expand(tempDir, "*.yml,,");
        assertEquals(1, results.size());
    }
}
