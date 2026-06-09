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
package io.polychro.ruleset;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ViolationTest {

    @Test
    void ofShouldCreateViolationWithNullPath() {
        Violation v = Violation.of("boom");
        assertEquals("boom", v.message());
        assertNull(v.path());
    }

    @Test
    void atShouldCreateViolationWithRelativePath() {
        Violation v = Violation.at("boom", "consumes[1].namespace");
        assertEquals("boom", v.message());
        assertEquals("consumes[1].namespace", v.path());
    }

    @Test
    void atShouldAllowNullPath() {
        Violation v = Violation.at("boom", null);
        assertEquals("boom", v.message());
        assertNull(v.path());
    }
}
