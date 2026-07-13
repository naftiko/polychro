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
package io.polychro.format.common;

/**
 * A reference (link or asset) collected from a projected document.
 *
 * @param target     the raw target as written in the source (e.g. {@code #intro}, {@code ./a.md},
 *                   {@code https://example.com}), never {@code null}
 * @param kind       the {@link LinkKind} classification of {@code target}
 * @param path       the JsonPath of the projected node carrying the reference
 * @param fragment   the fragment component of {@code target} (without the leading {@code #}), or
 *                   {@code null} when there is no fragment
 * @param filePart   the path component of {@code target} stripped of any fragment, never
 *                   {@code null} (empty for fragment-only references)
 */
public record LinkReference(String target, LinkKind kind, String path, String fragment, String filePart) {

    public LinkReference {
        if (target == null) {
            throw new IllegalArgumentException("target must not be null");
        }
        if (kind == null) {
            throw new IllegalArgumentException("kind must not be null");
        }
        if (path == null) {
            throw new IllegalArgumentException("path must not be null");
        }
        if (filePart == null) {
            throw new IllegalArgumentException("filePart must not be null");
        }
    }
}
