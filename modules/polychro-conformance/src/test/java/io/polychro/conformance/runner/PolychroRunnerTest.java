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
package io.polychro.conformance.runner;

import io.polychro.conformance.runner.PolychroRunner;
import io.polychro.conformance.runner.SpectralRunner;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link PolychroRunner#toComparablePath}, the one piece of non-trivial string
 * logic in this module (three prefix branches, a regex replace, and a conditional
 * {@code substring(1)}) that turns a Polychro diagnostic path into the dotted, bracket-free
 * segment form {@link SpectralRunner} produces — a mistake here silently turns a real
 * divergence into a false match, or vice versa.
 */
class PolychroRunnerTest {

    @Test
    void toComparablePathShouldReturnEmptyStringWhenPathIsNull() {
        assertEquals("", PolychroRunner.toComparablePath(null));
    }

    @Test
    void toComparablePathShouldReturnEmptyStringWhenPathIsBlank() {
        assertEquals("", PolychroRunner.toComparablePath("   "));
    }

    @Test
    void toComparablePathShouldReturnEmptyStringWhenPathIsRootOnly() {
        assertEquals("", PolychroRunner.toComparablePath("$"));
    }

    @Test
    void toComparablePathShouldStripDollarDotPrefixForObjectPath() {
        assertEquals("info.description", PolychroRunner.toComparablePath("$.info.description"));
    }

    @Test
    void toComparablePathShouldConvertBracketedArrayIndexToDottedSegment() {
        assertEquals("consumes.0.baseUri", PolychroRunner.toComparablePath("$.consumes[0].baseUri"));
    }

    @Test
    void toComparablePathShouldNotInsertLeadingSeparatorForRootArrayIndex() {
        // "$[0].name" starts with "$[", not "$.": the leading array-index segment must not gain
        // an extra separator, so it matches Spectral's own ["0", "name"] -> "0.name" normalization.
        assertEquals("0.name", PolychroRunner.toComparablePath("$[0].name"));
    }
}
