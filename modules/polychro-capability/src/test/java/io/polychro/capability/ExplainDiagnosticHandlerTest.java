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
 */package io.polychro.capability;

import com.fasterxml.jackson.databind.JsonNode;
import io.ikanos.engine.step.StepHandlerContext;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExplainDiagnosticHandlerTest {

    @Test
    void executeShouldReturnExplanationForKnownCode() {
        Map<String, ExplainDiagnosticHandler.RuleExplanation> explanations = Map.of(
                "missing-field", new ExplainDiagnosticHandler.RuleExplanation(
                        "A required field is missing from the document.",
                        "Add the missing field to the document root."
                )
        );

        ExplainDiagnosticHandler handler = new ExplainDiagnosticHandler(explanations);

        StepHandlerContext context = new TestStepHandlerContext(
                Map.of("code", "missing-field"), Map.of(), Map.of()
        );

        JsonNode result = handler.execute(context);

        assertEquals("missing-field", result.get("code").asText());
        assertEquals("A required field is missing from the document.",
                result.get("explanation").asText());
        assertEquals("Add the missing field to the document root.",
                result.get("suggestion").asText());
    }

    @Test
    void executeShouldReturnFallbackForUnknownCode() {
        Map<String, ExplainDiagnosticHandler.RuleExplanation> explanations = Map.of();

        ExplainDiagnosticHandler handler = new ExplainDiagnosticHandler(explanations);

        StepHandlerContext context = new TestStepHandlerContext(
                Map.of("code", "unknown-rule"), Map.of(), Map.of()
        );

        JsonNode result = handler.execute(context);

        assertEquals("unknown-rule", result.get("code").asText());
        assertEquals("No explanation available for code: unknown-rule",
                result.get("explanation").asText());
        assertEquals("Check the Polychro documentation for details.",
                result.get("suggestion").asText());
    }

    @Test
    void executeShouldHandleNullCode() {
        Map<String, ExplainDiagnosticHandler.RuleExplanation> explanations = Map.of(
                "some-rule", new ExplainDiagnosticHandler.RuleExplanation("Explain", "Fix")
        );

        ExplainDiagnosticHandler handler = new ExplainDiagnosticHandler(explanations);

        Map<String, Object> params = new HashMap<>();
        params.put("code", null);
        StepHandlerContext context = new TestStepHandlerContext(params, Map.of(), Map.of());

        JsonNode result = handler.execute(context);

        assertEquals("", result.get("code").asText());
        assertEquals("No explanation available for code: null",
                result.get("explanation").asText());
        assertEquals("Check the Polychro documentation for details.",
                result.get("suggestion").asText());
    }
}
