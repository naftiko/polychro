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
package io.polychro.spi;

/**
 * Maps projected document paths back to source ranges.
 */
@FunctionalInterface
public interface SourceMap {

    SourceMap NONE = path -> null;

    /**
     * Resolve a projected path to the original source range.
     *
     * @param path projected path or JsonPath-like selector
     * @return the corresponding source range, or null when unavailable
     */
    SourceRange resolve(String path);

    /**
     * Resolve a projected path to the source range of its <em>key</em> (the object field name /
     * property name the value at {@code path} is keyed by in its parent) rather than the value
     * itself.
     *
     * <p>Used by a JSONPath key-selector match ({@code ~}, naftiko/polychro#83): the
     * diagnostic reports on the property <em>name</em> (e.g. a Paths Object key like
     * {@code "/Pets"}), so its {@link SourceRange} must point at that key's own source location,
     * not at the path-item object's location a plain {@link #resolve(String)} would return.
     *
     * <p>Default implementation delegates to {@link #resolve(String)} — a source map that does
     * not track key locations separately (e.g. a lambda-based test double, or {@link #NONE})
     * degrades gracefully to the value's range rather than {@code null}, preserving the prior
     * behavior for every implementation that has not opted into key tracking.
     *
     * @param path projected path of the value the key belongs to
     * @return the key's source range, or the value's range as a fallback, or null when unavailable
     */
    default SourceRange resolveKey(String path) {
        return resolve(path);
    }
}
