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
 * Interface for ruleset functions that evaluate a node against configured options.
 */
public interface RuleFunction {

    /**
     * @return the function name as used in ruleset YAML (e.g. "truthy", "pattern")
     */
    String name();

    /**
     * Evaluate the given target node.
     *
     * @param targetNode the resolved JSON node to evaluate (may be null if field is missing)
     * @param options    function-specific options from the rule action
     * @return list of error messages; empty if the node passes
     */
    List<String> evaluate(JsonNode targetNode, Map<String, Object> options);

    /**
     * Evaluate the given target node and report violations that may pinpoint a node
     * <em>relative</em> to {@code targetNode} (issue #32, Layer 1).
     *
     * <p>The default implementation adapts {@link #evaluate(JsonNode, Map)}, mapping each
     * message to a {@link Violation} with a {@code null} path — i.e. the diagnostic's range
     * is that of the matched node. Built-in functions, which evaluate exactly the matched
     * node, rely on this default and need no change. Functions that inspect a subtree and
     * can locate a deeper offender (e.g. polyglot scripts returning {@code {message, path}})
     * override this method to carry a relative path.
     *
     * @param targetNode the resolved JSON node to evaluate (may be null if field is missing)
     * @param options    function-specific options from the rule action
     * @return list of violations; empty if the node passes
     */
    default List<Violation> evaluateViolations(JsonNode targetNode, Map<String, Object> options) {
        return evaluate(targetNode, options).stream()
                .map(Violation::of)
                .toList();
    }
}
