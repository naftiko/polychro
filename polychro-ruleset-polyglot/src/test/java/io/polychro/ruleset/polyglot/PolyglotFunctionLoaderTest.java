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
package io.polychro.ruleset.polyglot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PolyglotFunctionLoaderTest {

    private static final Path FUNCTIONS_DIR = Path.of("src/test/resources/functions").toAbsolutePath();

    @Test
    void loadFunctionsShouldLoadValidJsFile() {
        PolyglotFunctionLoader loader = new PolyglotFunctionLoader();
        Map<String, PolyglotRuleFunction> functions = loader.loadFunctions(FUNCTIONS_DIR, List.of("simple-check"));
        loader.close();

        assertEquals(1, functions.size());
        assertTrue(functions.containsKey("simple-check"));
        assertEquals("simple-check", functions.get("simple-check").name());
    }

    @Test
    void loadFunctionsShouldLoadMultipleFunctions() {
        PolyglotFunctionLoader loader = new PolyglotFunctionLoader();
        Map<String, PolyglotRuleFunction> functions = loader.loadFunctions(FUNCTIONS_DIR,
                List.of("simple-check", "multi-result", "unique-namespaces"));
        loader.close();

        assertEquals(3, functions.size());
    }

    @Test
    void loadFunctionsShouldSkipMissingFile() {
        PolyglotFunctionLoader loader = new PolyglotFunctionLoader();
        Map<String, PolyglotRuleFunction> functions = loader.loadFunctions(FUNCTIONS_DIR, List.of("nonexistent"));
        loader.close();

        assertTrue(functions.isEmpty());
    }

    @Test
    void loadFunctionsShouldSkipEmptyFile() {
        PolyglotFunctionLoader loader = new PolyglotFunctionLoader();
        Map<String, PolyglotRuleFunction> functions = loader.loadFunctions(FUNCTIONS_DIR, List.of("empty-file"));
        loader.close();

        assertTrue(functions.isEmpty());
    }

    @Test
    void loadFunctionsShouldSkipUnsupportedExtension(@TempDir Path tempDir) throws IOException {
        // Create a file with only .rb extension — loader won't find .js/.py/.groovy
        Files.writeString(tempDir.resolve("ruby-fn.rb"), "puts 'hello'");
        PolyglotFunctionLoader loader = new PolyglotFunctionLoader();
        Map<String, PolyglotRuleFunction> functions = loader.loadFunctions(tempDir, List.of("ruby-fn"));
        loader.close();

        assertTrue(functions.isEmpty());
    }

    @Test
    void resolveScriptFileShouldFindJsFirst() {
        PolyglotFunctionLoader loader = new PolyglotFunctionLoader();
        Path result = loader.resolveScriptFile(FUNCTIONS_DIR, "simple-check");
        loader.close();

        assertNotNull(result);
        assertTrue(result.toString().endsWith(".js"));
    }

    @Test
    void resolveScriptFileShouldReturnNullWhenNotFound() {
        PolyglotFunctionLoader loader = new PolyglotFunctionLoader();
        Path result = loader.resolveScriptFile(FUNCTIONS_DIR, "does-not-exist");
        loader.close();

        assertNull(result);
    }

    @Test
    void detectLanguageShouldReturnJsForJsFile() {
        assertEquals("js", PolyglotFunctionLoader.detectLanguage(Path.of("func.js")));
    }

    @Test
    void detectLanguageShouldReturnPythonForPyFile() {
        assertEquals("python", PolyglotFunctionLoader.detectLanguage(Path.of("func.py")));
    }

    @Test
    void detectLanguageShouldReturnGroovyForGroovyFile() {
        assertEquals("groovy", PolyglotFunctionLoader.detectLanguage(Path.of("func.groovy")));
    }

    @Test
    void detectLanguageShouldReturnNullForUnsupported() {
        assertNull(PolyglotFunctionLoader.detectLanguage(Path.of("func.rb")));
    }

    @Test
    void loadFunctionsShouldHandleIoError(@TempDir Path tempDir) throws IOException {
        Path lockedFile = tempDir.resolve("locked.js");
        Files.writeString(lockedFile, "export default function locked(x) { return []; }");

        boolean posix = FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
        if (posix) {
            // On POSIX systems (Linux/macOS in CI): remove all permissions so readString throws IOException
            Files.setPosixFilePermissions(lockedFile, PosixFilePermissions.fromString("---------"));
            try {
                PolyglotFunctionLoader loader = new PolyglotFunctionLoader();
                Map<String, PolyglotRuleFunction> functions = loader.loadFunctions(tempDir, List.of("locked"));
                loader.close();
                assertTrue(functions.isEmpty());
            } finally {
                // Restore permissions so TempDir cleanup can delete the file
                Files.setPosixFilePermissions(lockedFile, PosixFilePermissions.fromString("rw-------"));
            }
        } else {
            // On Windows: exclusive file lock prevents readString from succeeding
            try (var channel = java.nio.channels.FileChannel.open(lockedFile,
                    java.nio.file.StandardOpenOption.WRITE,
                    java.nio.file.StandardOpenOption.READ);
                 var lock = channel.lock()) {
                PolyglotFunctionLoader loader = new PolyglotFunctionLoader();
                Map<String, PolyglotRuleFunction> functions = loader.loadFunctions(tempDir, List.of("locked"));
                loader.close();
                assertTrue(functions.isEmpty());
            }
        }
    }
}
