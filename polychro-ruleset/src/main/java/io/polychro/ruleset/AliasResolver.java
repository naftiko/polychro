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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves alias references in rule {@code given} expressions.
 * <p>
 * Aliases are reusable JSONPath expressions referenced as {@code #AliasName} in rule
 * {@code given} fields. Supports:
 * <ul>
 *   <li>Global aliases: simple name → JSONPath mapping</li>
 *   <li>Alias chaining: {@code #AliasName.property} appends to the resolved path</li>
 *   <li>Scoped aliases: format-targeted aliases with {@code targets} array</li>
 * </ul>
 */
class AliasResolver {

    private static final Pattern ALIAS_PATTERN = Pattern.compile("^#([A-Za-z][A-Za-z0-9_-]*)(.*)$");

    /**
     * Resolve aliases in a list of given expressions.
     *
     * @param givenExpressions the list of given paths (may contain alias references)
     * @param aliases          the alias map (alias name → JSONPath expression)
     * @return the resolved list with aliases substituted
     * @throws RulesetParseException if an alias reference cannot be resolved
     */
    List<String> resolve(List<String> givenExpressions, Map<String, String> aliases) {
        if (givenExpressions == null || givenExpressions.isEmpty()) {
            return List.of();
        }
        if (aliases == null || aliases.isEmpty()) {
            return givenExpressions;
        }
        return givenExpressions.stream()
                .map(expr -> resolveExpression(expr, aliases))
                .toList();
    }

    /**
     * Resolve a single expression. If it starts with {@code #}, resolve as alias reference.
     */
    String resolveExpression(String expression, Map<String, String> aliases) {
        if (expression == null || !expression.startsWith("#")) {
            return expression;
        }

        Matcher matcher = ALIAS_PATTERN.matcher(expression);
        if (!matcher.matches()) {
            return expression;
        }

        String aliasName = matcher.group(1);
        String suffix = matcher.group(2); // e.g., ".property" or ""

        String resolved = aliases.get(aliasName);
        if (resolved == null) {
            throw new RulesetParseException("Unknown alias: #" + aliasName);
        }

        // If the resolved path itself is an alias reference, resolve recursively
        if (resolved.startsWith("#")) {
            resolved = resolveExpression(resolved, aliases);
        }

        if (suffix.isEmpty()) {
            return resolved;
        }

        // Append the suffix (chaining) — e.g., resolved="$.info" + ".title" -> "$.info.title"
        return resolved + suffix;
    }
}
