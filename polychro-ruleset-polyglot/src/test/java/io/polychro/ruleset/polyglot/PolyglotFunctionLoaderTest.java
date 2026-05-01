package io.polychro.ruleset.polyglot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
        // Create a valid .js file but lock it exclusively so readString fails on Windows
        Path lockedFile = tempDir.resolve("locked.js");
        Files.writeString(lockedFile, "export default function locked(x) { return []; }");

        try (var channel = java.nio.channels.FileChannel.open(lockedFile,
                java.nio.file.StandardOpenOption.WRITE,
                java.nio.file.StandardOpenOption.READ);
             var lock = channel.lock()) {
            PolyglotFunctionLoader loader = new PolyglotFunctionLoader();
            Map<String, PolyglotRuleFunction> functions = loader.loadFunctions(tempDir, List.of("locked"));
            loader.close();

            // On Windows, exclusive lock causes IOException in readString → function skipped
            assertTrue(functions.isEmpty());
        }
    }
}
