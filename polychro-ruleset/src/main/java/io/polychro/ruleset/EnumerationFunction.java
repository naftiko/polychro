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
 * Enumeration function — asserts the target value is one of the allowed values.
 * <p>
 * Options:
 * <ul>
 *     <li>{@code values} — list of allowed values</li>
 * </ul>
 */
class EnumerationFunction implements RuleFunction {

    @Override
    public String name() {
        return "enumeration";
    }

    @Override
    public List<String> evaluate(JsonNode targetNode, Map<String, Object> options) {
        if (targetNode == null || targetNode.isNull() || targetNode.isMissingNode()) {
            return List.of("Value must not be null");
        }

        Object valuesObj = options.get("values");
        if (!(valuesObj instanceof List<?> allowedValues)) {
            return List.of("Enumeration function requires a 'values' option");
        }

        if (allowedValues.isEmpty()) {
            return List.of("Value is not in the allowed list");
        }

        String targetValue = targetNode.asText();
        for (Object allowed : allowedValues) {
            if (allowed != null && allowed.toString().equals(targetValue)) {
                return List.of();
            }
        }

        return List.of("Value \"" + targetValue + "\" is not in the allowed list");
    }
}
