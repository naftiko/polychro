package io.polychro.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.polychro.spi.Diagnostic;

import java.util.List;

/**
 * Formats diagnostics as a JSON array.
 */
public class JsonFormatter implements DiagnosticFormatter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String format(List<Diagnostic> diagnostics) {
        ArrayNode array = MAPPER.createArrayNode();

        if (diagnostics != null) {
            for (Diagnostic d : diagnostics) {
                ObjectNode node = MAPPER.createObjectNode();
                node.put("severity", d.severity().name().toLowerCase());
                node.put("message", d.message());
                if (d.code() != null) {
                    node.put("code", d.code());
                }
                if (d.path() != null) {
                    node.put("path", d.path());
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
            }
        }

        return array.toPrettyString();
    }
}
