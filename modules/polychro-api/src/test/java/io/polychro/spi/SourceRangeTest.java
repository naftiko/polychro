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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SourceRangeTest {

    @Test
    void constructionShouldStoreAllFields() {
        SourceRange range = new SourceRange(1, 5, 3, 20);
        assertEquals(1, range.startLine());
        assertEquals(5, range.startColumn());
        assertEquals(3, range.endLine());
        assertEquals(20, range.endColumn());
    }

    @Test
    void equalityShouldWorkForIdenticalValues() {
        SourceRange a = new SourceRange(1, 1, 1, 10);
        SourceRange b = new SourceRange(1, 1, 1, 10);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equalityShouldFailForDifferentValues() {
        SourceRange a = new SourceRange(1, 1, 1, 10);
        SourceRange b = new SourceRange(2, 1, 2, 10);
        assertNotEquals(a, b);
    }

    @Test
    void edgeValueZero() {
        SourceRange range = new SourceRange(0, 0, 0, 0);
        assertEquals(0, range.startLine());
        assertEquals(0, range.startColumn());
    }

    @Test
    void edgeValueMaxInt() {
        SourceRange range = new SourceRange(Integer.MAX_VALUE, Integer.MAX_VALUE,
                Integer.MAX_VALUE, Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, range.startLine());
        assertEquals(Integer.MAX_VALUE, range.endColumn());
    }
}
