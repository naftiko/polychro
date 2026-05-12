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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.polychro.spi.Diagnostic;
import io.polychro.spi.Severity;

import java.util.List;

/**
 * Formats diagnostics as LLM-native JSON output — token-efficient, structured,
 * and directly actionable by AI agents in generate-validate-retry loops.
 * <p>
 * Includes a summary with error/warning/info counts and an estimated token cost.
 */
public class AgentFormatter implements DiagnosticFormatter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String format(List<Diagnostic> diagnostics) {
        ObjectNode root = MAPPER.createObjectNode();
        ArrayNode array = MAPPER.createArrayNode();

        int errors = 0;
        int warnings = 0;
        int infos = 0;

        if (diagnostics != null) {
            for (Diagnostic d : diagnostics) {
                ObjectNode node = MAPPER.createObjectNode();
                node.put("severity", mapSeverity(d.severity()));

                if (d.path() != null) {
                    node.put("path", d.path());
                }
                if (d.code() != null) {
                    node.put("rule", d.code());
                }
                node.put("message", d.message());

                String suggestion = deriveSuggestion(d);
                if (suggestion != null) {
                    node.put("suggestion", suggestion);
                }

                if (d.range() != null) {
                    ObjectNode range = MAPPER.createObjectNode();
                    range.put("startLine", d.range().startLine());
                    range.put("startColumn", d.range().startColumn());
                    range.put("endLine", d.range().endLine());
                    range.put("endColumn", d.range().endColumn());
                    node.set("range", range);
                }

                array.add(node);

                switch (d.severity()) {
                    case ERROR -> errors++;
                    case WARN -> warnings++;
                    default -> infos++;
                }
            }
        }

        root.set("diagnostics", array);

        ObjectNode summary = MAPPER.createObjectNode();
        summary.put("errors", errors);
        summary.put("warnings", warnings);
        summary.put("info", infos);
        root.set("summary", summary);

        // Compute token estimate on the JSON without the "tokens" field, then add it.
        // This ensures the returned JSON and the counted JSON are the same object.
        String jsonWithoutTokens = root.toPrettyString();
        root.put("tokens", estimateTokens(jsonWithoutTokens));

        return root.toPrettyString();
    }

    static String mapSeverity(Severity severity) {
        return switch (severity) {
            case ERROR -> "error";
            case WARN -> "warning";
            case INFO -> "info";
            case HINT -> "hint";
        };
    }

    static String deriveSuggestion(Diagnostic diagnostic) {
        String code = diagnostic.code();
        String message = diagnostic.message();
        if (code == null || message == null) {
            return null;
        }
        // Generate actionable suggestion from the diagnostic message and code
        if (message.contains("must not") || message.contains("should not")) {
            return "Fix: " + message;
        }
        if (message.contains("missing") || message.contains("Missing")) {
            return "Add the " + extractSubject(message);
        }
        if (message.contains("invalid") || message.contains("Invalid")) {
            return "Correct the " + extractSubject(message);
        }
        return null;
    }

    private static String extractSubject(String message) {
        // Extract the relevant subject from the message
        int colonIndex = message.indexOf(':');
        if (colonIndex > 0 && colonIndex < message.length() - 1) {
            return message.substring(colonIndex + 1).trim();
        }
        return message.toLowerCase();
    }

    static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // Approximate token count: ~4 characters per token for English/JSON
        // This is a simplified estimation aligned with GPT tokenizer averages
        return (text.length() + 3) / 4;
    }
}
