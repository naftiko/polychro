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
