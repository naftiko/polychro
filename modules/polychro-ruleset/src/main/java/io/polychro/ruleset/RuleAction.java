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

import java.util.Map;

/**
 * An action to apply when a rule matches (the "then" clause).
 *
 * @param field           optional field path to target within the matched node
 * @param functionName    name of the function to invoke
 * @param functionOptions options passed to the function
 */
record RuleAction(
        String field,
        String functionName,
        Map<String, Object> functionOptions
) {
    RuleAction {
        functionOptions = functionOptions != null ? Map.copyOf(functionOptions) : Map.of();
    }
}
