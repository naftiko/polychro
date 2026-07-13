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
package io.polychro.html;

import io.polychro.spi.SourceRange;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HtmlSourceMapBuilderTest {

    @Test
    void putShouldStoreRangeAndPrecision() {
        HtmlSourceMapBuilder builder = new HtmlSourceMapBuilder();
        SourceRange range = new SourceRange(1, 1, 2, 2);
        builder.put("$.document.nodes[0]", range, HtmlSourceMapBuilder.PRECISION_EXACT);
        assertEquals(range, builder.build().resolve("$.document.nodes[0]"));
        assertEquals(HtmlSourceMapBuilder.PRECISION_EXACT, builder.precisionOf("$.document.nodes[0]"));
    }

    @Test
    void putShouldDefaultPrecisionToApproximateWhenNull() {
        HtmlSourceMapBuilder builder = new HtmlSourceMapBuilder();
        builder.put("a", new SourceRange(1, 1, 1, 1), null);
        assertEquals(HtmlSourceMapBuilder.PRECISION_APPROXIMATE, builder.precisionOf("a"));
    }

    @Test
    void putShouldIgnoreNullPathOrRange() {
        HtmlSourceMapBuilder builder = new HtmlSourceMapBuilder();
        builder.put(null, new SourceRange(1, 1, 1, 1), HtmlSourceMapBuilder.PRECISION_EXACT);
        builder.put("x", null, HtmlSourceMapBuilder.PRECISION_EXACT);
        assertNull(builder.build().resolve("x"));
        assertNull(builder.precisionOf("x"));
    }
}
