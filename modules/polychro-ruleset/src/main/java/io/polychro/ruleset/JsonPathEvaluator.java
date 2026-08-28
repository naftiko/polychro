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
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
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
 *
 * <p>Also supports the Spectral/{@code jsonpath-plus} key-selector operator: a {@code given}
 * expression ending in {@code ~} (e.g. {@code $.paths.*~}) matches the property <em>names</em> of
 * the nodes selected by the expression with the trailing {@code ~} stripped, instead of their
 * values — see {@link #evaluate(JsonNode, String)}. Plain Jayway JSONPath has no such operator, so
 * this is a small pre/post-processing layer on top of it (naftiko/polychro#83).
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
    private static final Pattern BRACKET_SEGMENT = Pattern.compile("\\['(.*?)'\\]|\\[(\\d+)\\]");

    /**
     * Matches the trailing Spectral/{@code jsonpath-plus} key-selector operator ({@code ~}) at the
     * end of a {@code given} expression (ignoring trailing whitespace). Jayway JSONPath never uses
     * {@code ~} anywhere in its own grammar, so any expression ending in it is unambiguously a
     * key-selector expression, not a false positive from an unrelated feature.
     */
    private static final Pattern KEY_SELECTOR_SUFFIX = Pattern.compile("~\\s*$");

    /**
     * Evaluate a JSONPath expression against the given document root.
     *
     * <p>An expression ending in the key-selector operator {@code ~} (e.g. {@code $.paths.*~})
     * returns the property <em>names</em> (or array indices) of the nodes matched by the
     * expression with {@code ~} stripped, instead of their values — mirroring Spectral /
     * {@code jsonpath-plus} semantics (naftiko/polychro#83). String keys are returned as text
     * nodes, numeric array indices as integer nodes.
     *
     * @param root       the document root node
     * @param expression the JSONPath expression (e.g. "$.info.name"), optionally ending in {@code ~}
     * @return the list of matched nodes; empty if no matches or expression is invalid
     */
    List<JsonNode> evaluate(JsonNode root, String expression) {
        if (root == null || expression == null || expression.isBlank()) {
            return List.of();
        }
        if (isKeySelector(expression)) {
            return evaluateKeySelector(root, stripKeySelector(expression));
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
     * <p>For a key-selector expression (ending in {@code ~}), the returned paths are those of the
     * underlying matched <em>nodes</em> (the {@code ~} is stripped before evaluation) — the path
     * still identifies the node the key belongs to (a key has no path of its own), but the caller
     * (see {@code RuleExecutor}) resolves the SourceRange for these paths via
     * {@link io.polychro.spi.SourceMap#resolveKey} rather than {@code resolve}, so the reported
     * range points at the key's own source location, not the value's.
     *
     * @param root       the document root node
     * @param expression the JSONPath expression, optionally ending in {@code ~}
     * @return the concrete paths of the matched nodes; empty if no matches or expression is invalid
     */
    List<String> evaluatePaths(JsonNode root, String expression) {
        if (root == null || expression == null || expression.isBlank()) {
            return List.of();
        }
        if (isKeySelector(expression)) {
            return evaluatePaths(root, stripKeySelector(expression));
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
     * @return {@code true} when {@code expression} ends in the key-selector operator {@code ~}
     *         (ignoring trailing whitespace)
     */
    static boolean isKeySelector(String expression) {
        return expression != null && KEY_SELECTOR_SUFFIX.matcher(expression).find();
    }

    /**
     * Strips the trailing key-selector operator {@code ~} (and any trailing whitespace) from
     * {@code expression}, returning the plain JSONPath expression underneath.
     */
    static String stripKeySelector(String expression) {
        return KEY_SELECTOR_SUFFIX.matcher(expression).replaceFirst("");
    }

    /**
     * Resolves a key-selector expression: evaluates {@code baseExpression} for its raw
     * (bracket-notation) concrete paths, then extracts the last path segment of each — the
     * property name (or array index) the matched node is keyed by in its parent — as the "value"
     * of that match, per Spectral/{@code jsonpath-plus} {@code ~} semantics.
     */
    private List<JsonNode> evaluateKeySelector(JsonNode root, String baseExpression) {
        List<String> rawPaths = rawPaths(root, baseExpression);
        List<JsonNode> keys = new ArrayList<>(rawPaths.size());
        for (String rawPath : rawPaths) {
            keys.add(lastSegmentAsNode(rawPath));
        }
        return keys;
    }

    /**
     * Evaluate {@code expression} and return the raw Jayway bracket-notation concrete path of each
     * match (e.g. {@code $['paths']['/pets']}), before any dot-notation conversion — needed to
     * extract the exact last segment (string key or numeric index) without the dot-ambiguity that
     * {@link #toDotNotation} accepts as a limitation elsewhere.
     */
    private List<String> rawPaths(JsonNode root, String expression) {
        try {
            ArrayNode pathNodes = JsonPath.using(PATH_CONFIG).parse(root).read(expression);
            List<String> paths = new ArrayList<>(pathNodes.size());
            for (JsonNode node : pathNodes) {
                paths.add(node.asText());
            }
            return paths;
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Extracts the last bracket segment of a raw Jayway path ({@code $['a']['b'][0]}) as a
     * {@link JsonNode}: a string key becomes a {@link TextNode}, a numeric array index becomes an
     * {@link IntNode}. Returns a JSON {@code null} ({@link NullNode}) for a path with no segments
     * (e.g. the root {@code $} itself has no "own key") — mirroring {@code jsonpath-plus}, which
     * returns the root's (nonexistent) parent-property value, i.e. {@code undefined}/{@code null},
     * for {@code $~}. An empty {@link TextNode} would instead be a truthy, matchable string and
     * silently change {@code truthy}/{@code pattern}/etc. results for this degenerate case.
     */
    static JsonNode lastSegmentAsNode(String bracketPath) {
        Matcher matcher = BRACKET_SEGMENT.matcher(bracketPath);
        String lastKey = null;
        String lastIndex = null;
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                lastKey = matcher.group(1);
                lastIndex = null;
            } else {
                lastIndex = matcher.group(2);
                lastKey = null;
            }
        }
        if (lastIndex != null) {
            return IntNode.valueOf(Integer.parseInt(lastIndex));
        }
        if (lastKey != null) {
            return TextNode.valueOf(lastKey);
        }
        return NullNode.getInstance();
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
