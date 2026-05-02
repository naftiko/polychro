package io.polychro.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.naftiko.engine.step.StepHandler;
import io.naftiko.engine.step.StepHandlerContext;

import java.util.Map;

/**
 * Step handler for the "do-explain" step. Provides an explanation and fix suggestion
 * for a given diagnostic code (rule name).
 */
class ExplainDiagnosticHandler implements StepHandler {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Map<String, RuleExplanation> explanations;

    ExplainDiagnosticHandler(Map<String, RuleExplanation> explanations) {
        this.explanations = explanations;
    }

    @Override
    public JsonNode execute(StepHandlerContext context) {
        String code = (String) context.inputParameter("code");

        ObjectNode result = MAPPER.createObjectNode();
        result.put("code", code != null ? code : "");

        RuleExplanation explanation = code != null ? explanations.get(code) : null;
        if (explanation != null) {
            result.put("explanation", explanation.explanation());
            result.put("suggestion", explanation.suggestion());
        } else {
            result.put("explanation", "No explanation available for code: " + code);
            result.put("suggestion", "Check the Polychro documentation for details.");
        }

        return result;
    }

    record RuleExplanation(String explanation, String suggestion) {
    }
}
