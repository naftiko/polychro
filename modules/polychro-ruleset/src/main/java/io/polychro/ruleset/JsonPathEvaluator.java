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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
     * JSONPath configuration that returns the concrete path of each match (e.g.
     * {@code $['consumes'][0]['baseUri']}) instead of the matched value.
     */
    private static final Configuration PATH_CONFIG = Configuration.builder()
            .jsonProvider(new JacksonJsonNodeJsonProvider())
            .mappingProvider(new JacksonMappingProvider())
            .options(Option.ALWAYS_RETURN_LIST, Option.SUPPRESS_EXCEPTIONS, Option.AS_PATH_LIST)
            .build();

    /** Matches a single bracketed segment: {@code ['key']} or {@code [0]}. */
    private static final Pattern BRACKET_SEGMENT = Pattern.compile("\\['([^']*)'\\]|\\[(\\d+)\\]");

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

    /**
     * Evaluate a JSONPath expression and return the concrete dot-notation path of each match.
     *
     * <p>The returned paths are normalized to dot notation (e.g. {@code $.consumes[0].baseUri}),
     * matching the keys produced by {@link io.polychro.spi.JacksonSourceMap}, so a path can be used
     * both as the diagnostic path and as a source-map lookup key. The order and cardinality mirror
     * {@link #evaluate(JsonNode, String)} for the same expression and document.
     *
     * @param root       the document root node
     * @param expression the JSONPath expression
     * @return the concrete paths of the matched nodes; empty if no matches or expression is invalid
     */
    List<String> evaluatePaths(JsonNode root, String expression) {
        if (root == null || expression == null || expression.isBlank()) {
            return List.of();
        }
        try {
            ArrayNode pathNodes = JsonPath.using(PATH_CONFIG).parse(root).read(expression);
            List<String> paths = new ArrayList<>(pathNodes.size());
            for (JsonNode node : pathNodes) {
                paths.add(toDotNotation(node.asText()));
            }
            return paths;
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Convert a Jayway bracket-notation path ({@code $['a'][0]['b']}) to dot notation
     * ({@code $.a[0].b}). Numeric segments stay bracketed; named keys become {@code .key}.
     *
     * <p><strong>Keys containing a dot are ambiguous in the output.</strong> A key such as
     * {@code "x-meta.owner"} becomes {@code $.x-meta.owner}, which is indistinguishable from the
     * nested path {@code x-meta -> owner}. This matches the keying used by
     * {@code JacksonSourceMap}, so the two round-trip consistently, but it means a source-range
     * lookup for a path that traverses a dotted key cannot be guaranteed precise (see that class's
     * Javadoc). This is an accepted limitation of dot-notation keying.
     */
    static String toDotNotation(String bracketPath) {
        if (bracketPath == null || bracketPath.isEmpty()) {
            return bracketPath;
        }
        Matcher matcher = BRACKET_SEGMENT.matcher(bracketPath);
        StringBuilder sb = new StringBuilder("$");
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                sb.append('.').append(matcher.group(1));
            } else {
                sb.append('[').append(matcher.group(2)).append(']');
            }
        }
        return sb.toString();
    }
}
