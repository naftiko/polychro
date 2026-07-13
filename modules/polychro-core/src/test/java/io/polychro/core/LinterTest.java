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
package io.polychro.core;

import io.polychro.spi.Diagnostic;
import io.polychro.spi.Document;
import io.polychro.spi.Severity;
import io.polychro.spi.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LinterTest {

    @Test
    void lintShouldReturnEmptyForNoValidators() {
        Linter linter = new Linter(List.of(), false);
        Document doc = Document.fromString("{}", "json");
        List<Diagnostic> result = linter.lint(doc);
        assertTrue(result.isEmpty());
    }

    @Test
    void lintShouldCollectDiagnosticsFromSingleValidator() {
        Diagnostic diag = new Diagnostic(Severity.WARN, "test-code", "test msg", "$.root", null);
        Validator mockValidator = new StubValidator("test", List.of(diag));
        Linter linter = new Linter(List.of(mockValidator), false);
        Document doc = Document.fromString("{}", "json");

        List<Diagnostic> result = linter.lint(doc);
        assertEquals(1, result.size());
        assertEquals("test msg", result.get(0).message());
    }

    @Test
    void lintShouldCollectDiagnosticsFromMultipleValidators() {
        Diagnostic d1 = new Diagnostic(Severity.ERROR, "c1", "msg1", "$.a", null);
        Diagnostic d2 = new Diagnostic(Severity.WARN, "c2", "msg2", "$.b", null);
        Validator v1 = new StubValidator("v1", List.of(d1));
        Validator v2 = new StubValidator("v2", List.of(d2));
        Linter linter = new Linter(List.of(v1, v2), false);
        Document doc = Document.fromString("{}", "json");

        List<Diagnostic> result = linter.lint(doc);
        assertEquals(2, result.size());
    }

    @Test
    void lintShouldSortResultsBySeverityThenPath() {
        Diagnostic warn = new Diagnostic(Severity.WARN, "c1", "msg1", "$.a", null);
        Diagnostic error = new Diagnostic(Severity.ERROR, "c2", "msg2", "$.b", null);
        Validator v = new StubValidator("v", List.of(warn, error));
        Linter linter = new Linter(List.of(v), false);
        Document doc = Document.fromString("{}", "json");

        List<Diagnostic> result = linter.lint(doc);
        assertEquals(Severity.ERROR, result.get(0).severity());
        assertEquals(Severity.WARN, result.get(1).severity());
    }

    @Test
    void lintShouldDeduplicateDiagnostics() {
        Diagnostic d1 = new Diagnostic(Severity.WARN, "c1", "msg", "$.x", null);
        Diagnostic d2 = new Diagnostic(Severity.ERROR, "c2", "msg", "$.x", null);
        Validator v1 = new StubValidator("v1", List.of(d1));
        Validator v2 = new StubValidator("v2", List.of(d2));
        Linter linter = new Linter(List.of(v1, v2), false);
        Document doc = Document.fromString("{}", "json");

        List<Diagnostic> result = linter.lint(doc);
        assertEquals(1, result.size());
        assertEquals(Severity.ERROR, result.get(0).severity());
    }

    @Test
    void lintShouldStopOnErrorWhenFailFastEnabled() {
        Diagnostic error = new Diagnostic(Severity.ERROR, "c1", "stop", "$.a", null);
        Diagnostic warn = new Diagnostic(Severity.WARN, "c2", "continue", "$.b", null);
        Validator v1 = new StubValidator("v1", List.of(error));
        Validator v2 = new StubValidator("v2", List.of(warn));
        Linter linter = new Linter(List.of(v1, v2), true);
        Document doc = Document.fromString("{}", "json");

        List<Diagnostic> result = linter.lint(doc);
        assertEquals(1, result.size());
        assertEquals("stop", result.get(0).message());
    }

    @Test
    void lintShouldContinueOnWarnWhenFailFastEnabled() {
        Diagnostic warn = new Diagnostic(Severity.WARN, "c1", "warn", "$.a", null);
        Diagnostic info = new Diagnostic(Severity.INFO, "c2", "info", "$.b", null);
        Validator v1 = new StubValidator("v1", List.of(warn));
        Validator v2 = new StubValidator("v2", List.of(info));
        Linter linter = new Linter(List.of(v1, v2), true);
        Document doc = Document.fromString("{}", "json");

        List<Diagnostic> result = linter.lint(doc);
        assertEquals(2, result.size());
    }

    @Test
    void validatorsShouldReturnImmutableList() {
        Validator v = new StubValidator("v", List.of());
        Linter linter = new Linter(List.of(v), false);
        assertThrows(UnsupportedOperationException.class, () -> linter.validators().add(v));
    }

    @Test
    void builderShouldCreateLinterInstance() {
        Linter linter = Linter.builder().build();
        assertNotNull(linter);
        assertTrue(linter.validators().isEmpty());
    }

    private static class StubValidator implements Validator {
        private final String name;
        private final List<Diagnostic> diagnostics;

        StubValidator(String name, List<Diagnostic> diagnostics) {
            this.name = name;
            this.diagnostics = diagnostics;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public List<Diagnostic> validate(Document doc) {
            return diagnostics;
        }
    }
}
