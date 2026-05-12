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
