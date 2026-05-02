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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Registry of built-in rule functions and custom functions discovered via {@link FunctionProvider} SPI.
 */
class BuiltinFunctions {

    private static final Map<String, RuleFunction> FUNCTIONS = new LinkedHashMap<>();

    static {
        register(new TruthyFunction());
        register(new FalsyFunction());
        register(new DefinedFunction());
        register(new UndefinedFunction());
        register(new PatternFunction());
        register(new EnumerationFunction());
        register(new LengthFunction());
        register(new SchemaFunction());
        register(new AlphabeticalFunction());
        register(new CasingFunction());
        register(new XorFunction());
        register(new TypedEnumFunction());

        // Discover custom functions via SPI
        ServiceLoader<FunctionProvider> providers = ServiceLoader.load(FunctionProvider.class);
        for (FunctionProvider provider : providers) {
            for (RuleFunction function : provider.functions()) {
                FUNCTIONS.put(function.name(), function);
            }
        }
    }

    private static void register(RuleFunction function) {
        FUNCTIONS.put(function.name(), function);
    }

    /**
     * Look up a function by name.
     *
     * @param name the function name (case-sensitive)
     * @return the function, or empty if not found
     */
    static Optional<RuleFunction> get(String name) {
        return Optional.ofNullable(FUNCTIONS.get(name));
    }

    /**
     * @return all registered function names
     */
    static List<String> names() {
        return List.copyOf(FUNCTIONS.keySet());
    }

    private BuiltinFunctions() {
    }
}
