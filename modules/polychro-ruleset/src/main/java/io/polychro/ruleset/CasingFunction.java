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

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Casing function — asserts the target string matches a specific naming convention.
 * <p>
 * Options:
 * <ul>
 *     <li>{@code type} — one of: camel, pascal, kebab, snake, cobol, macro</li>
 * </ul>
 */
class CasingFunction implements RuleFunction {

    private static final Map<String, Pattern> PATTERNS = Map.of(
            "camel", Pattern.compile("^[a-z][a-zA-Z0-9]*$"),
            "pascal", Pattern.compile("^[A-Z][a-zA-Z0-9]*$"),
            "kebab", Pattern.compile("^[a-z][a-z0-9]*(-[a-z0-9]+)*$"),
            "snake", Pattern.compile("^[a-z][a-z0-9]*(_[a-z0-9]+)*$"),
            "cobol", Pattern.compile("^[A-Z][A-Z0-9]*(-[A-Z0-9]+)*$"),
            "macro", Pattern.compile("^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$")
    );

    @Override
    public String name() {
        return "casing";
    }

    @Override
    public List<String> evaluate(JsonNode targetNode, Map<String, Object> options) {
        if (targetNode == null || targetNode.isNull() || targetNode.isMissingNode()) {
            return List.of("Value must be a string for casing check");
        }
        if (!targetNode.isTextual()) {
            return List.of("Value must be a string for casing check");
        }

        String value = targetNode.asText();
        if (value.isEmpty()) {
            return List.of();
        }

        Object typeObj = options.get("type");
        if (typeObj == null) {
            return List.of("Casing function requires a 'type' option");
        }

        String type = typeObj.toString();
        Pattern pattern = PATTERNS.get(type);
        if (pattern == null) {
            return List.of("Unknown casing type: " + type);
        }

        if (!pattern.matcher(value).matches()) {
            return List.of("Value \"" + value + "\" does not match " + type + " casing");
        }

        return List.of();
    }
}
