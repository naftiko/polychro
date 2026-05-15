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
 * An anchor or id declared within a projected document.
 *
 * <p>Carries the textual identifier (already slugified for Markdown headings, taken from the
 * {@code id} attribute for HTML elements), the JsonPath of the declaring node within the
 * projected document, and a free-form origin tag (e.g. {@code "heading"}, {@code "element"}).
 *
 * @param id     the anchor identifier, never {@code null}
 * @param path   the JsonPath of the declaring node within the projected document, never
 *               {@code null}
 * @param origin a free-form origin tag describing where the anchor was found
 */
public record Anchor(String id, String path, String origin) {

    public Anchor {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        if (path == null) {
            throw new IllegalArgumentException("path must not be null");
        }
    }
}
