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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MarkdownSourceMapBuilderTest {

    @Test
    void putShouldIgnoreNullPath() {
        MarkdownSourceMapBuilder builder = new MarkdownSourceMapBuilder();

        builder.put(null, new SourceRange(1, 1, 1, 1));

        assertNull(builder.build().resolve("$.document.headings[0]"));
    }

    @Test
    void putShouldIgnoreNullRange() {
        MarkdownSourceMapBuilder builder = new MarkdownSourceMapBuilder();

        builder.put("$.document.headings[0]", null);

        assertNull(builder.build().resolve("$.document.headings[0]"));
    }

    @Test
    void buildShouldResolveStoredPaths() {
        MarkdownSourceMapBuilder builder = new MarkdownSourceMapBuilder();
        SourceRange range = new SourceRange(2, 1, 2, 5);

        builder.put("$.document.headings[0]", range);
        SourceMap sourceMap = builder.build();

        assertEquals(range, sourceMap.resolve("$.document.headings[0]"));
    }
}
