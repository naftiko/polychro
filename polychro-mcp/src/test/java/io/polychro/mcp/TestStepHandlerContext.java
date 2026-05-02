package io.polychro.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import io.naftiko.engine.step.StepHandlerContext;

import java.util.Map;

/**
 * Test implementation of StepHandlerContext for unit testing handlers.
 */
class TestStepHandlerContext implements StepHandlerContext {

    private final Map<String, Object> inputParameters;
    private final Map<String, JsonNode> stepOutputs;
    private final Map<String, Object> withValues;

    TestStepHandlerContext(Map<String, Object> inputParameters,
                           Map<String, JsonNode> stepOutputs,
                           Map<String, Object> withValues) {
        this.inputParameters = inputParameters;
        this.stepOutputs = stepOutputs;
        this.withValues = withValues;
    }

    @Override
    public Map<String, Object> inputParameters() {
        return inputParameters;
    }

    @Override
    public Map<String, JsonNode> stepOutputs() {
        return stepOutputs;
    }

    @Override
    public Map<String, Object> withValues() {
        return withValues;
    }

    @Override
    public JsonNode stepOutput(String stepName) {
        return stepOutputs.get(stepName);
    }

    @Override
    public Object inputParameter(String name) {
        return inputParameters.get(name);
    }
}
