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
        return List.of(new TestFunction());
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
}
