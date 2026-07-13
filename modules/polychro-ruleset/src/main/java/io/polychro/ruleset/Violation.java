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

/**
 * A single violation reported by a {@link RuleFunction}.
 *
 * <p>The {@code message} is the human-readable description of the problem. The optional
 * {@code path} is a <em>relative</em> path, expressed in dot/bracket notation
 * (e.g. {@code consumes[0].namespace}), that pinpoints the offending node <em>within</em>
 * the node the rule matched. It is appended to the matched node's path to form the
 * concrete path resolved against the document {@link io.polychro.spi.SourceMap} (issue #32).
 *
 * <p>Built-in functions evaluate exactly the matched node and therefore leave {@code path}
 * {@code null}: the diagnostic's range is that of the matched node itself, preserving the
 * pre-existing behaviour. Custom functions (e.g. polyglot scripts) that inspect a subtree
 * and report a deeper offender set {@code path} so the diagnostic can point at the precise
 * location rather than the whole matched subtree.
 *
 * @param message the human-readable violation message (never {@code null})
 * @param path    a relative path to the offending node within the matched node, or
 *                {@code null} to use the matched node's own location
 */
public record Violation(String message, String path) {

    /**
     * Create a violation with no relative path (range resolves to the matched node).
     *
     * @param message the violation message
     * @return a violation whose {@code path} is {@code null}
     */
    public static Violation of(String message) {
        return new Violation(message, null);
    }

    /**
     * Create a violation that pinpoints a node relative to the matched node.
     *
     * @param message the violation message
     * @param path    a relative path (dot/bracket notation), or {@code null}
     * @return a violation
     */
    public static Violation at(String message, String path) {
        return new Violation(message, path);
    }
}
