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
 * Truthy function — asserts the target value is "truthy" (non-null, non-empty, non-zero, non-false).
 */
class TruthyFunction implements RuleFunction {

    @Override
    public String name() {
        return "truthy";
    }

    @Override
    public List<String> evaluate(JsonNode targetNode, Map<String, Object> options) {
        if (isFalsy(targetNode)) {
            return List.of("Value must be truthy");
        }
        return List.of();
    }

    static boolean isFalsy(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return true;
        }
        if (node.isTextual() && node.asText().isEmpty()) {
            return true;
        }
        if (node.isNumber() && node.numberValue().doubleValue() == 0.0) {
            return true;
        }
        if (node.isBoolean() && !node.asBoolean()) {
            return true;
        }
        if (node.isArray() && node.isEmpty()) {
            return true;
        }
        if (node.isObject() && node.isEmpty()) {
            return true;
        }
        return false;
    }
}
