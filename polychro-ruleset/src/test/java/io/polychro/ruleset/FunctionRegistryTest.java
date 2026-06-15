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

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FunctionRegistryTest {

    @Test
    void shouldResolveBuiltinFunctions() {
        FunctionRegistry registry = FunctionRegistry.forRuleset(null, List.of());
        assertTrue(registry.get("truthy").isPresent());
        assertTrue(registry.get("pattern").isPresent());
    }

    @Test
    void shouldDiscoverCustomFunctionsViaServiceLoader() {
        FunctionRegistry registry = FunctionRegistry.forRuleset(null, List.of());
        Optional<RuleFunction> fn = registry.get("testCustomFunction");
        assertTrue(fn.isPresent());
    }

    @Test
    void shouldTreatNullFunctionNamesAsEmpty() {
        // Exercises the null-guard on the declared function names.
        FunctionRegistry registry = FunctionRegistry.forRuleset(null, null);
        assertTrue(registry.get("truthy").isPresent());
    }

    @Test
    void shouldReturnEmptyForUnknownFunction() {
        FunctionRegistry registry = FunctionRegistry.forRuleset(null, List.of());
        assertFalse(registry.get("nonexistent").isPresent());
    }

    @Test
    @SuppressWarnings("deprecation") // intentionally exercises the deprecated functions() default
    void functionProviderDefaultsToNoFunctions() {
        // A provider that implements neither method relies on the deprecated default functions()
        // (and the context-aware default delegating to it), contributing nothing.
        FunctionProvider bare = new FunctionProvider() {
        };
        assertTrue(bare.functions().isEmpty());
        assertTrue(bare.functions(null, List.of()).isEmpty());
    }
}
