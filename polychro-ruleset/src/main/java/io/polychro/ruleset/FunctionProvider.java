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
import java.util.List;
import java.util.ServiceLoader;

/**
 * SPI for providing custom rule functions.
 * Implementations are discovered via {@link ServiceLoader}.
 */
public interface FunctionProvider {

    /**
     * @return the list of rule functions provided by this implementation
     * @deprecated superseded by {@link #functions(Path, List)}, which supplies the ruleset's
     *             {@code functionsDir} and declared function names so providers discovered via
     *             {@link ServiceLoader} (and thus instantiated with a no-arg constructor) can
     *             load functions on demand. Retained for providers that are pre-configured with
     *             their own source and ignore the ruleset context.
     */
    @Deprecated
    default List<RuleFunction> functions() {
        return List.of();
    }

    /**
     * Provide the rule functions declared by a ruleset.
     *
     * <p>Called once per ruleset with the ruleset's {@code functionsDir} (the directory holding
     * custom function source files) and the list of declared {@code functions} names. A provider
     * that has no functions to contribute for this ruleset returns an empty list. The default
     * implementation delegates to the deprecated {@link #functions()} so existing pre-configured
     * providers keep working.
     *
     * @param functionsDir   the ruleset's custom-functions directory, or {@code null} if undeclared
     * @param functionNames  the function names declared by the ruleset (never {@code null})
     * @return the rule functions this provider contributes for the given ruleset
     */
    default List<RuleFunction> functions(Path functionsDir, List<String> functionNames) {
        return functions();
    }
}
