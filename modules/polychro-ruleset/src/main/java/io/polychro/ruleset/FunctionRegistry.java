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

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Per-ruleset registry resolving a function name to its {@link RuleFunction}.
 *
 * <p>It layers two sources: the built-in functions (always available) and the custom functions
 * declared by the ruleset, the latter discovered via the {@link FunctionProvider} SPI. Custom
 * functions are resolved <em>with the ruleset context</em> ({@code functionsDir} and the declared
 * {@code functions} names) so a provider discovered through {@link ServiceLoader} — and therefore
 * instantiated with a no-arg constructor — can load the right scripts for this ruleset (issue #32,
 * Layer 1). A custom function may override a built-in of the same name.
 */
class FunctionRegistry {

    private final Map<String, RuleFunction> functions;

    private FunctionRegistry(Map<String, RuleFunction> functions) {
        this.functions = functions;
    }

    /**
     * Build a registry for a ruleset.
     *
     * @param functionsDir  the ruleset's custom-functions directory, or {@code null} if undeclared
     * @param functionNames the function names declared by the ruleset (may be empty)
     * @return a registry combining built-ins with the ruleset's custom functions
     */
    static FunctionRegistry forRuleset(Path functionsDir, List<String> functionNames) {
        Map<String, RuleFunction> resolved = new LinkedHashMap<>();
        for (String name : BuiltinFunctions.names()) {
            BuiltinFunctions.get(name).ifPresent(fn -> resolved.put(fn.name(), fn));
        }

        List<String> names = functionNames != null ? functionNames : List.of();
        ServiceLoader<FunctionProvider> providers = ServiceLoader.load(FunctionProvider.class);
        for (FunctionProvider provider : providers) {
            for (RuleFunction function : provider.functions(functionsDir, names)) {
                resolved.put(function.name(), function);
            }
        }
        return new FunctionRegistry(resolved);
    }

    /**
     * Look up a function by name.
     *
     * @param name the function name (case-sensitive)
     * @return the function, or empty if neither a built-in nor a declared custom function
     */
    Optional<RuleFunction> get(String name) {
        return Optional.ofNullable(functions.get(name));
    }
}
