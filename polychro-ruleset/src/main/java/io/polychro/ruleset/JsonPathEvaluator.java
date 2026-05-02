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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Evaluates JSONPath expressions against a Jackson {@link JsonNode} document.
 */
class JsonPathEvaluator {

    private static final Configuration JSONPATH_CONFIG = Configuration.builder()
            .jsonProvider(new JacksonJsonNodeJsonProvider())
            .mappingProvider(new JacksonMappingProvider())
            .options(Option.ALWAYS_RETURN_LIST, Option.SUPPRESS_EXCEPTIONS)
            .build();

    /**
     * Evaluate a JSONPath expression against the given document root.
     *
     * @param root       the document root node
     * @param expression the JSONPath expression (e.g. "$.info.name")
     * @return the list of matched nodes; empty if no matches or expression is invalid
     */
    List<JsonNode> evaluate(JsonNode root, String expression) {
        if (root == null || expression == null || expression.isBlank()) {
            return List.of();
        }
        try {
            ArrayNode arrayNode = JsonPath.using(JSONPATH_CONFIG).parse(root).read(expression);
            List<JsonNode> nodes = new ArrayList<>(arrayNode.size());
            for (JsonNode node : arrayNode) {
                nodes.add(node);
            }
            return nodes;
        } catch (Exception e) {
            return List.of();
        }
    }
}
