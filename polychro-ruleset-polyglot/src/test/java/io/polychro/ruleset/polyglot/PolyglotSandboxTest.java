package io.polychro.ruleset.polyglot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graalvm.polyglot.Engine;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PolyglotSandboxTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static Engine engine;

    @BeforeAll
    static void setUp() {
        engine = Engine.create();
    }

    @AfterAll
    static void tearDown() {
        engine.close();
    }

    @Test
    void jsShouldBlockHostClassAccess() throws Exception {
        String source = """
                export default function hostAccess(targetVal) {
                  var runtime = java.lang.Runtime.getRuntime();
                  return [];
                }
                """;
        PolyglotRuleFunction fn = new PolyglotRuleFunction("host-access", source, "js", engine);
        JsonNode input = JSON.readTree("{}");

        List<String> results = fn.evaluate(input, Map.of());
        // Should fail with error message about blocked access
        assertFalse(results.isEmpty());
        assertTrue(results.get(0).contains("execution failed"));
    }

    @Test
    void jsShouldBlockFileSystemAccess() throws Exception {
        String source = """
                export default function fileAccess(targetVal) {
                  var fs = require('fs');
                  fs.readFileSync('/etc/passwd');
                  return [];
                }
                """;
        PolyglotRuleFunction fn = new PolyglotRuleFunction("file-access", source, "js", engine);
        JsonNode input = JSON.readTree("{}");

        List<String> results = fn.evaluate(input, Map.of());
        assertFalse(results.isEmpty());
        assertTrue(results.get(0).contains("execution failed"));
    }

    @Test
    void jsShouldBlockThreadCreation() throws Exception {
        String source = """
                export default function threadCreate(targetVal) {
                  new Thread(function() {}).start();
                  return [];
                }
                """;
        PolyglotRuleFunction fn = new PolyglotRuleFunction("thread-create", source, "js", engine);
        JsonNode input = JSON.readTree("{}");

        List<String> results = fn.evaluate(input, Map.of());
        assertFalse(results.isEmpty());
        assertTrue(results.get(0).contains("execution failed"));
    }
}
