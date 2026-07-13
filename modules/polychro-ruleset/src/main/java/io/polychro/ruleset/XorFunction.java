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

/**
 * XOR function — asserts that exactly one of the specified properties is present.
 * <p>
 * Options:
 * <ul>
 *     <li>{@code properties} — list of property names (exactly one must exist)</li>
 * </ul>
 */
class XorFunction implements RuleFunction {

    @Override
    public String name() {
        return "xor";
    }

    @Override
    public List<String> evaluate(JsonNode targetNode, Map<String, Object> options) {
        if (targetNode == null || targetNode.isNull() || targetNode.isMissingNode()) {
            return List.of("Value must be an object for xor check");
        }
        if (!targetNode.isObject()) {
            return List.of("Value must be an object for xor check");
        }

        Object propsObj = options.get("properties");
        if (!(propsObj instanceof List<?> properties)) {
            return List.of("XOR function requires a 'properties' option");
        }

        int count = 0;
        for (Object prop : properties) {
            if (prop != null && targetNode.has(prop.toString())) {
                JsonNode field = targetNode.get(prop.toString());
                if (!field.isNull()) {
                    count++;
                }
            }
        }

        if (count == 1) {
            return List.of();
        }

        if (count == 0) {
            return List.of("Exactly one of " + properties + " must be present, but none were found");
        }

        return List.of("Exactly one of " + properties + " must be present, but " + count + " were found");
    }
}
