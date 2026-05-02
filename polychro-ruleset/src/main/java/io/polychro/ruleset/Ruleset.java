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

import java.util.List;
import java.util.Map;

/**
 * A parsed Spectral-format ruleset.
 *
 * @param extendsRefs     list of base ruleset paths to inherit from
 * @param aliases         global alias map (alias name → JSONPath expression)
 * @param overrides       list of file-scoped rule overrides
 * @param formats         list of document format identifiers this ruleset applies to
 * @param functionsDir    directory path for custom function files
 * @param functions       list of custom function names declared
 * @param rules           rule definitions keyed by rule name
 * @param documentationUrl base URL for rule documentation
 */
record Ruleset(
        List<String> extendsRefs,
        Map<String, String> aliases,
        List<RulesetOverride> overrides,
        List<String> formats,
        String functionsDir,
        List<String> functions,
        Map<String, Rule> rules,
        String documentationUrl
) {
    Ruleset {
        extendsRefs = extendsRefs != null ? List.copyOf(extendsRefs) : List.of();
        aliases = aliases != null ? Map.copyOf(aliases) : Map.of();
        overrides = overrides != null ? List.copyOf(overrides) : List.of();
        formats = formats != null ? List.copyOf(formats) : List.of();
        functions = functions != null ? List.copyOf(functions) : List.of();
        rules = rules != null ? Map.copyOf(rules) : Map.of();
    }
}
