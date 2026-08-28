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
package io.polychro.ruleset;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * Test function provider to exercise the ServiceLoader discovery path in BuiltinFunctions.
 */
public class TestFunctionProvider implements FunctionProvider {

    @Override
    public List<RuleFunction> functions() {
        return List.of(new TestFunction(), new PathReportingFunction(), new MultiViolationFunction());
    }

    static class TestFunction implements RuleFunction {
        @Override
        public String name() {
            return "testCustomFunction";
        }

        @Override
        public List<String> evaluate(JsonNode targetNode, Map<String, Object> options) {
            return List.of();
        }
    }

    /**
     * A function that always reports one violation pinned to a relative path, used to exercise the
     * {@link RuleExecutor} path-combination + range-resolution branch (issue #32, Layer 1).
     */
    static class PathReportingFunction implements RuleFunction {
        @Override
        public String name() {
            return "testPathReportingFunction";
        }

        @Override
        public List<String> evaluate(JsonNode targetNode, Map<String, Object> options) {
            return List.of("offending child");
        }

        @Override
        public List<Violation> evaluateViolations(JsonNode targetNode, Map<String, Object> options) {
            return List.of(Violation.at("offending child", "name"));
        }
    }

    /**
     * A function that always reports two distinct violations for the same matched node, used to
     * exercise the {@code {{error}}} message-placeholder feature in {@link RuleExecutor}: without
     * opting in via {@code message: "{{error}}"}, a static rule message/description would collapse
     * both violations into indistinguishable diagnostics.
     */
    static class MultiViolationFunction implements RuleFunction {
        @Override
        public String name() {
            return "testMultiViolationFunction";
        }

        @Override
        public List<String> evaluate(JsonNode targetNode, Map<String, Object> options) {
            return List.of("first violation", "second violation");
        }
    }
}
