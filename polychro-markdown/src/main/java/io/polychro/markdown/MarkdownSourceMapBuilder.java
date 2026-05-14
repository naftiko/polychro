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
package io.polychro.markdown;

import io.polychro.spi.SourceMap;
import io.polychro.spi.SourceRange;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds a simple path-to-range source map for projected Markdown nodes.
 */
class MarkdownSourceMapBuilder {

    private final Map<String, SourceRange> ranges = new LinkedHashMap<>();

    void put(String path, SourceRange range) {
        if (path == null || range == null) {
            return;
        }
        ranges.put(path, range);
    }

    SourceMap build() {
        Map<String, SourceRange> snapshot = Map.copyOf(ranges);
        return snapshot::get;
    }
}