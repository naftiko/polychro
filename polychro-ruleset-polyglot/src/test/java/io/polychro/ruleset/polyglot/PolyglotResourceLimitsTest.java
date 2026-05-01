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

class PolyglotResourceLimitsTest {

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
    void jsInfiniteLoopShouldBeTerminated() throws Exception {
        String source = """
                export default function infiniteLoop(targetVal) {
                  while (true) {}
                  return [];
                }
                """;
        PolyglotRuleFunction fn = new PolyglotRuleFunction("infinite-loop", source, "js", engine);
        JsonNode input = JSON.readTree("{}");

        List<String> results = fn.evaluate(input, Map.of());
        assertFalse(results.isEmpty());
        assertTrue(results.get(0).contains("execution failed"));
    }
}
