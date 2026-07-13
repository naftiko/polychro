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
package io.polychro.ruleset.polyglot;

import io.polychro.ruleset.Violation;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Golden tests for the JS-result {@code path} → relative dot/bracket path normalisation
 * ({@link PolyglotRuleFunction#extractPath}) and the surrounding
 * {@link PolyglotRuleFunction#extractViolations} extraction (issue #32, Layer 1).
 *
 * <p>The Naftiko functions emit {@code path} as an array of segments
 * (e.g. {@code ["consumes", 0, "namespace"]}); the executor relies on this normalisation to
 * combine the segment path with the rule's {@code given} path and resolve a {@link
 * io.polychro.spi.SourceRange}. These tests pin every branch of that conversion.
 */
class PolyglotPathExtractionTest {

    private static Context context;

    @BeforeAll
    static void setUp() {
        context = Context.create("js");
    }

    @AfterAll
    static void tearDown() {
        context.close();
    }

    /** Evaluate a JS expression into a polyglot {@link Value} for direct unit testing. */
    private static Value js(String expression) {
        return context.eval("js", "(" + expression + ")");
    }

    // --- extractPath ---------------------------------------------------------------------------

    @Test
    void extractPathShouldReturnNullForNullValue() {
        assertNull(PolyglotRuleFunction.extractPath(null));
    }

    @Test
    void extractPathShouldReturnNullForJsNull() {
        assertNull(PolyglotRuleFunction.extractPath(js("null")));
    }

    @Test
    void extractPathShouldReturnStringAsIs() {
        assertEquals("consumes[0].namespace",
                PolyglotRuleFunction.extractPath(js("'consumes[0].namespace'")));
    }

    @Test
    void extractPathShouldReturnNullForEmptyString() {
        assertNull(PolyglotRuleFunction.extractPath(js("''")));
    }

    @Test
    void extractPathShouldReturnNullForNonArrayNonStringValue() {
        // A number is neither null, nor a string, nor an array.
        assertNull(PolyglotRuleFunction.extractPath(js("42")));
    }

    @Test
    void extractPathShouldJoinArraySegmentsWithDotsAndBrackets() {
        // Leading string segment has no dot; numeric segment becomes [i]; trailing string is dotted.
        assertEquals("consumes[1].namespace",
                PolyglotRuleFunction.extractPath(js("['consumes', 1, 'namespace']")));
    }

    @Test
    void extractPathShouldHandleNestedCapabilitySegments() {
        assertEquals("capability.exposes[2].namespace",
                PolyglotRuleFunction.extractPath(js("['capability', 'exposes', 2, 'namespace']")));
    }

    @Test
    void extractPathShouldReturnNullForEmptyArray() {
        assertNull(PolyglotRuleFunction.extractPath(js("[]")));
    }

    @Test
    void extractPathShouldSkipNonStringNonNumberSegments() {
        // A malformed segment (object) the Naftiko functions never emit is skipped, not thrown on;
        // the surrounding valid segments still form a usable path.
        assertEquals("consumes[0].namespace",
                PolyglotRuleFunction.extractPath(js("['consumes', 0, { bogus: true }, 'namespace']")));
    }

    @Test
    void extractPathShouldReturnNullWhenAllSegmentsAreSkipped() {
        assertNull(PolyglotRuleFunction.extractPath(js("[{ a: 1 }, true]")));
    }

    // --- extractViolations ---------------------------------------------------------------------

    @Test
    void extractViolationsShouldReturnEmptyForNullResult() {
        assertTrue(PolyglotRuleFunction.extractViolations(null).isEmpty());
    }

    @Test
    void extractViolationsShouldReturnEmptyForNonArrayResult() {
        assertTrue(PolyglotRuleFunction.extractViolations(js("42")).isEmpty());
    }

    @Test
    void extractViolationsShouldCaptureMessageAndNormalisedPath() {
        List<Violation> violations = PolyglotRuleFunction.extractViolations(
                js("[{ message: 'dup', path: ['consumes', 1, 'namespace'] }]"));

        assertEquals(1, violations.size());
        assertEquals("dup", violations.get(0).message());
        assertEquals("consumes[1].namespace", violations.get(0).path());
    }

    @Test
    void extractViolationsShouldLeavePathNullWhenAbsent() {
        List<Violation> violations =
                PolyglotRuleFunction.extractViolations(js("[{ message: 'no path here' }]"));

        assertEquals(1, violations.size());
        assertEquals("no path here", violations.get(0).message());
        assertNull(violations.get(0).path());
    }

    @Test
    void extractViolationsShouldSkipItemsWithoutMessage() {
        List<Violation> violations = PolyglotRuleFunction.extractViolations(
                js("[{ path: ['a'] }, { message: 'kept' }, { other: true }]"));

        assertEquals(1, violations.size());
        assertEquals("kept", violations.get(0).message());
    }
}
