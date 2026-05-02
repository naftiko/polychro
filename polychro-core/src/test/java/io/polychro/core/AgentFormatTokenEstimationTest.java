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
package io.polychro.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.polychro.spi.Diagnostic;
import io.polychro.spi.Severity;
import io.polychro.spi.SourceRange;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AgentFormatTokenEstimationTest {

    private final AgentFormatter formatter = new AgentFormatter();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void estimateTokensShouldReturnZeroForNull() {
        assertEquals(0, AgentFormatter.estimateTokens(null));
    }

    @Test
    void estimateTokensShouldReturnZeroForEmpty() {
        assertEquals(0, AgentFormatter.estimateTokens(""));
    }

    @Test
    void estimateTokensShouldReturnLowCountForShortText() {
        // "hello" = 5 chars → (5+3)/4 = 2 tokens
        assertEquals(2, AgentFormatter.estimateTokens("hello"));
    }

    @Test
    void estimateTokensShouldReturnHigherCountForLongText() {
        // 100 chars → (100+3)/4 = 25 tokens
        String text = "a".repeat(100);
        assertEquals(25, AgentFormatter.estimateTokens(text));
    }

    @Test
    void estimateTokensShouldHandleSingleCharacter() {
        // 1 char → (1+3)/4 = 1 token
        assertEquals(1, AgentFormatter.estimateTokens("x"));
    }

    @Test
    void formatShouldIncludeTokenCount() throws Exception {
        Diagnostic d = new Diagnostic(Severity.ERROR, "rule", "msg", null, null);

        String result = formatter.format(List.of(d));
        JsonNode root = mapper.readTree(result);

        assertTrue(root.has("tokens"));
        int tokens = root.get("tokens").asInt();
        assertTrue(tokens > 0);
    }

    @Test
    void tokenCountShouldScaleWithDiagnosticCount() throws Exception {
        Diagnostic d = new Diagnostic(Severity.WARN, "rule", "warning message", null, null);

        String singleResult = formatter.format(List.of(d));
        String multiResult = formatter.format(List.of(d, d, d, d, d));

        JsonNode singleRoot = mapper.readTree(singleResult);
        JsonNode multiRoot = mapper.readTree(multiResult);

        int singleTokens = singleRoot.get("tokens").asInt();
        int multiTokens = multiRoot.get("tokens").asInt();

        assertTrue(multiTokens > singleTokens);
    }

    @Test
    void emptyDiagnosticsShouldHaveMinimalTokenCount() throws Exception {
        String result = formatter.format(List.of());
        JsonNode root = mapper.readTree(result);

        int tokens = root.get("tokens").asInt();
        // Empty diagnostics still have the JSON structure overhead
        assertTrue(tokens > 0);
        assertTrue(tokens < 50); // Should be small
    }

    @Test
    void longMessageShouldIncreaseTokenCount() throws Exception {
        String shortMsg = "err";
        String longMsg = "This is a very long error message ".repeat(10);

        String shortResult = formatter.format(List.of(
                new Diagnostic(Severity.ERROR, "r", shortMsg, null, null)));
        String longResult = formatter.format(List.of(
                new Diagnostic(Severity.ERROR, "r", longMsg, null, null)));

        JsonNode shortRoot = mapper.readTree(shortResult);
        JsonNode longRoot = mapper.readTree(longResult);

        assertTrue(longRoot.get("tokens").asInt() > shortRoot.get("tokens").asInt());
    }

    @Test
    void diagnosticWithRangeAndPathShouldHaveMoreTokens() throws Exception {
        Diagnostic minimal = new Diagnostic(Severity.ERROR, "r", "msg", null, null);
        Diagnostic full = new Diagnostic(Severity.ERROR, "r", "msg",
                "$.consumes[0].operations[1].path",
                new SourceRange(42, 12, 42, 24));

        String minResult = formatter.format(List.of(minimal));
        String fullResult = formatter.format(List.of(full));

        JsonNode minRoot = mapper.readTree(minResult);
        JsonNode fullRoot = mapper.readTree(fullResult);

        assertTrue(fullRoot.get("tokens").asInt() > minRoot.get("tokens").asInt());
    }

    @Test
    void multilineSuggestionShouldBeCountedInTokens() throws Exception {
        Diagnostic d = new Diagnostic(Severity.ERROR, "rule",
                "Path must not end with '/' and should not contain spaces",
                null, null);

        String result = formatter.format(List.of(d));
        JsonNode root = mapper.readTree(result);

        // Suggestion is included, increasing token count
        assertTrue(root.get("diagnostics").get(0).has("suggestion"));
        assertTrue(root.get("tokens").asInt() > 30);
    }
}
