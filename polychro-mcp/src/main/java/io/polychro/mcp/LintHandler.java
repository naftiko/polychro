package io.polychro.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.naftiko.engine.step.StepHandler;
import io.naftiko.engine.step.StepHandlerContext;
import io.polychro.core.Linter;
import io.polychro.spi.Diagnostic;
import io.polychro.spi.Document;
import io.polychro.spi.Severity;

import java.util.List;

/**
 * Step handler for the "do-lint" step. Runs the full Polychro linting pipeline
 * on a document provided as a string input parameter.
 */
class LintHandler implements StepHandler {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Linter linter;

    LintHandler(Linter linter) {
        this.linter = linter;
    }

    @Override
    public JsonNode execute(StepHandlerContext context) {
        String content = (String) context.inputParameter("document");
        String format = (String) context.inputParameter("format");
        String sourcePath = (String) context.inputParameter("source-path");

        Document doc = Document.fromString(content, format, sourcePath);
        List<Diagnostic> diagnostics = linter.lint(doc);

        return buildResult(diagnostics);
    }

    static JsonNode buildResult(List<Diagnostic> diagnostics) {
        ObjectNode result = MAPPER.createObjectNode();
        ArrayNode array = MAPPER.createArrayNode();

        for (Diagnostic d : diagnostics) {
            ObjectNode entry = MAPPER.createObjectNode();
            entry.put("severity", d.severity().name());
            if (d.code() != null) {
                entry.put("code", d.code());
            }
            entry.put("message", d.message());
            if (d.path() != null) {
                entry.put("path", d.path());
            }
            array.add(entry);
        }

        result.set("diagnostics", array);
        result.put("count", diagnostics.size());
        boolean hasErrors = diagnostics.stream()
                .anyMatch(d -> d.severity() == Severity.ERROR);
        result.put("has-errors", hasErrors);

        return result;
    }
}
