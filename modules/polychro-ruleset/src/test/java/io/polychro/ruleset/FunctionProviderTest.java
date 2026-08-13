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
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FunctionProviderTest {

    /**
     * A fake function name used to verify the bridging default forwards the declared
     * {@link Function} names to {@link FunctionProvider#functions(Path, List)}.
     */
    private static final String FAKE_FUNCTION_NAME = "legacyPathBasedFunction";

    @Test
    void functionsWithFunctionListShouldBridgeToPathBasedOverloadWhenOnlyThatOneIsOverridden() {
        // A provider that only overrides functions(Path, List<String>) — the contract in place
        // before this ruleset's functionsByFilename-based overload was introduced — must still
        // contribute its functions when called through the new functions(List<Function>) entry
        // point used by FunctionRegistry. The bridging default must not silently fall through to
        // the deprecated no-arg functions() default (which would return an empty list here).
        FunctionProvider legacyProvider = new FunctionProvider() {
            @Override
            public List<RuleFunction> functions(Path functionsDir, List<String> functionNames) {
                assertTrue(functionNames.contains(FAKE_FUNCTION_NAME),
                        "Expected the declared function name to be forwarded, got: " + functionNames);
                return List.of(new FakeRuleFunction(FAKE_FUNCTION_NAME));
            }
        };

        List<Function> declared = List.of(new Function(FAKE_FUNCTION_NAME + ".js", FAKE_FUNCTION_NAME, ""));
        List<RuleFunction> contributed = legacyProvider.functions(declared);

        assertEquals(1, contributed.size());
        assertEquals(FAKE_FUNCTION_NAME, contributed.getFirst().name());
    }

    @Test
    void functionsWithFunctionListShouldFallThroughToDeprecatedDefaultWhenNoOverloadIsOverridden() {
        // A provider overriding none of the three functions(...) methods keeps contributing
        // nothing — the deprecated no-arg default is the terminal fallback of the bridging chain.
        FunctionProvider bareProvider = new FunctionProvider() {
        };

        assertTrue(bareProvider.functions(List.of(new Function("f.js", "f", ""))).isEmpty());
    }

    @Test
    void functionsWithFunctionListShouldTreatNullListAsEmpty() {
        // functions(List<Function>) must be null-safe on its own, independent of callers (like
        // FunctionRegistry) that already normalize null to List.of() before dispatching.
        FunctionProvider legacyProvider = new FunctionProvider() {
            @Override
            public List<RuleFunction> functions(Path functionsDir, List<String> functionNames) {
                assertTrue(functionNames.isEmpty(), "Expected no function names for a null list");
                return List.of(new FakeRuleFunction(FAKE_FUNCTION_NAME));
            }
        };

        assertEquals(1, legacyProvider.functions(null).size());
    }

    private static final class FakeRuleFunction implements RuleFunction {
        private final String name;

        private FakeRuleFunction(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public List<String> evaluate(JsonNode targetNode, Map<String, Object> options) {
            return List.of();
        }
    }
}
