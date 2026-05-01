package io.polychro.ruleset;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PatternFunctionTest {

    private final PatternFunction fn = new PatternFunction();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void nameShouldReturnPattern() {
        assertEquals("pattern", fn.name());
    }

    @Test
    void evaluateShouldPassWhenMatchHits() {
        JsonNode node = mapper.valueToTree("hello-world");
        var result = fn.evaluate(node, Map.of("match", "^hello"));
        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldFailWhenMatchMisses() {
        JsonNode node = mapper.valueToTree("world-hello");
        var result = fn.evaluate(node, Map.of("match", "^hello"));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldPassWhenNotMatchMisses() {
        JsonNode node = mapper.valueToTree("hello-world");
        var result = fn.evaluate(node, Map.of("notMatch", "^world"));
        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldFailWhenNotMatchHits() {
        JsonNode node = mapper.valueToTree("world-hello");
        var result = fn.evaluate(node, Map.of("notMatch", "^world"));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForNonStringValue() {
        JsonNode node = mapper.valueToTree(42);
        var result = fn.evaluate(node, Map.of("match", "\\d+"));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForNullValue() {
        var result = fn.evaluate(null, Map.of("match", ".*"));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForNullNode() {
        JsonNode node = mapper.nullNode();
        var result = fn.evaluate(node, Map.of("match", ".*"));
        assertFalse(result.isEmpty());
    }

    @Test
    void evaluateShouldReportInvalidMatchRegex() {
        JsonNode node = mapper.valueToTree("test");
        var result = fn.evaluate(node, Map.of("match", "[invalid"));
        assertFalse(result.isEmpty());
        assertTrue(result.get(0).contains("Invalid regex"));
    }

    @Test
    void evaluateShouldReportInvalidNotMatchRegex() {
        JsonNode node = mapper.valueToTree("test");
        var result = fn.evaluate(node, Map.of("notMatch", "[invalid"));
        assertFalse(result.isEmpty());
        assertTrue(result.get(0).contains("Invalid regex"));
    }

    @Test
    void evaluateShouldPassWithNoOptions() {
        JsonNode node = mapper.valueToTree("test");
        var result = fn.evaluate(node, Map.of());
        assertTrue(result.isEmpty());
    }

    @Test
    void evaluateShouldFailForMissingNode() {
        JsonNode node = mapper.missingNode();
        var result = fn.evaluate(node, Map.of("match", ".*"));
        assertFalse(result.isEmpty());
    }
}
