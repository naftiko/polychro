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
}