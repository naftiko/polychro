package io.polychro.spi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SeverityTest {

    @Test
    void valuesShouldReturnAllFourLevels() {
        Severity[] values = Severity.values();
        assertEquals(4, values.length);
        assertEquals(Severity.ERROR, values[0]);
        assertEquals(Severity.WARN, values[1]);
        assertEquals(Severity.INFO, values[2]);
        assertEquals(Severity.HINT, values[3]);
    }

    @Test
    void valueOfShouldReturnCorrectEnum() {
        assertEquals(Severity.ERROR, Severity.valueOf("ERROR"));
        assertEquals(Severity.WARN, Severity.valueOf("WARN"));
        assertEquals(Severity.INFO, Severity.valueOf("INFO"));
        assertEquals(Severity.HINT, Severity.valueOf("HINT"));
    }

    @Test
    void valueOfShouldThrowForInvalidName() {
        assertThrows(IllegalArgumentException.class, () -> Severity.valueOf("UNKNOWN"));
    }

    @Test
    void ordinalShouldReflectSeverityOrder() {
        assertTrue(Severity.ERROR.ordinal() < Severity.WARN.ordinal());
        assertTrue(Severity.WARN.ordinal() < Severity.INFO.ordinal());
        assertTrue(Severity.INFO.ordinal() < Severity.HINT.ordinal());
    }
}
