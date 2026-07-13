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
