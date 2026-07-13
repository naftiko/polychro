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
 * Classification of a link or asset reference within a projected document.
 *
 * <ul>
 *   <li>{@link #INTERNAL_ANCHOR} — fragment-only reference (e.g. {@code #section}).</li>
 *   <li>{@link #RELATIVE_FILE} — relative path on disk (e.g. {@code ../sibling.md}), optionally
 *       carrying a fragment.</li>
 *   <li>{@link #EXTERNAL} — absolute HTTP(S) URL.</li>
 *   <li>{@link #MAILTO} — {@code mailto:} URI.</li>
 *   <li>{@link #TEL} — {@code tel:} URI.</li>
 *   <li>{@link #DATA} — {@code data:} URI.</li>
 *   <li>{@link #JAVASCRIPT} — {@code javascript:} URI.</li>
 *   <li>{@link #EMPTY} — empty or whitespace-only reference.</li>
 *   <li>{@link #MALFORMED} — non-empty reference that does not match any known shape.</li>
 * </ul>
 */
public enum LinkKind {
    INTERNAL_ANCHOR,
    RELATIVE_FILE,
    EXTERNAL,
    MAILTO,
    TEL,
    DATA,
    JAVASCRIPT,
    EMPTY,
    MALFORMED
}
