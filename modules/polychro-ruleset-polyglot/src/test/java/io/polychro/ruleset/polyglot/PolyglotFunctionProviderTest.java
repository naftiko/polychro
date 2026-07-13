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
package io.polychro.ruleset.polyglot;

import io.polychro.ruleset.FunctionProvider;
import io.polychro.ruleset.RuleFunction;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.*;

class PolyglotFunctionProviderTest {

    private static final Path FUNCTIONS_DIR = Path.of("src/test/resources/functions").toAbsolutePath();

    @Test
    void functionsShouldReturnEmptyForDefaultConstructor() {
        PolyglotFunctionProvider provider = new PolyglotFunctionProvider();
        List<RuleFunction> functions = provider.functions();
        assertTrue(functions.isEmpty());
    }

    @Test
    void functionsShouldLoadJsFunctions() {
        PolyglotFunctionProvider provider = PolyglotFunctionProvider.forDirectory(
                FUNCTIONS_DIR, List.of("simple-check", "multi-result"));
        List<RuleFunction> functions = provider.functions();

        assertEquals(2, functions.size());
    }

    @Test
    void functionsShouldLoadNaftikoFunctions() {
        PolyglotFunctionProvider provider = PolyglotFunctionProvider.forDirectory(
                FUNCTIONS_DIR, List.of("unique-namespaces", "control-port-validation", "aggregate-semantics-consistency"));
        List<RuleFunction> functions = provider.functions();

        assertEquals(3, functions.size());
    }

    @Test
    void functionsShouldSkipUnknownFunctionName() {
        PolyglotFunctionProvider provider = PolyglotFunctionProvider.forDirectory(
                FUNCTIONS_DIR, List.of("nonexistent-func"));
        List<RuleFunction> functions = provider.functions();

        assertTrue(functions.isEmpty());
    }

    @Test
    void functionsShouldReturnEmptyWhenFunctionNamesEmpty() {
        PolyglotFunctionProvider provider = PolyglotFunctionProvider.forDirectory(FUNCTIONS_DIR, List.of());
        List<RuleFunction> functions = provider.functions();
        assertTrue(functions.isEmpty());
    }

    @Test
    void providerShouldBeDiscoverableViaServiceLoader() {
        ServiceLoader<FunctionProvider> loader = ServiceLoader.load(FunctionProvider.class);
        boolean found = false;
        for (FunctionProvider provider : loader) {
            if (provider instanceof PolyglotFunctionProvider) {
                found = true;
                break;
            }
        }
        assertTrue(found, "PolyglotFunctionProvider should be discoverable via ServiceLoader");
    }

    // --- context-aware functions(Path, names) — the production wiring path (issue #32) ----------

    @Test
    void contextAwareFunctionsShouldLoadFromSuppliedContext() {
        // The no-arg ServiceLoader instance carries no state; the ruleset supplies dir + names.
        PolyglotFunctionProvider provider = new PolyglotFunctionProvider();
        List<RuleFunction> functions =
                provider.functions(FUNCTIONS_DIR, List.of("simple-check", "multi-result"));

        assertEquals(2, functions.size());
    }

    @Test
    void contextAwareFunctionsShouldFallBackToPreconfiguredContext() {
        // No ruleset context supplied (null/null) → fall back to the forDirectory()-supplied state.
        PolyglotFunctionProvider provider = PolyglotFunctionProvider.forDirectory(
                FUNCTIONS_DIR, List.of("simple-check"));
        List<RuleFunction> functions = provider.functions(null, null);

        assertEquals(1, functions.size());
    }

    @Test
    void contextAwareFunctionsShouldReturnEmptyWhenNoDirectoryResolvable() {
        // No-arg provider (dir == null) and no context supplied → nothing to load.
        PolyglotFunctionProvider provider = new PolyglotFunctionProvider();
        assertTrue(provider.functions(null, List.of("simple-check")).isEmpty());
    }

    @Test
    void contextAwareFunctionsShouldReturnEmptyWhenNamesResolveEmpty() {
        // Directory resolvable but no names anywhere (supplied empty, none pre-configured).
        PolyglotFunctionProvider provider = new PolyglotFunctionProvider();
        assertTrue(provider.functions(FUNCTIONS_DIR, List.of()).isEmpty());
    }
}
