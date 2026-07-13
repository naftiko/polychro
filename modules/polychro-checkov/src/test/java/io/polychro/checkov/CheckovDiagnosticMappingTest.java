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
package io.polychro.checkov;

import io.polychro.spi.Diagnostic;
import io.polychro.spi.Severity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CheckovDiagnosticMappingTest {

    private final DiagnosticMapper mapper = new DiagnosticMapper();

    @Test
    void mapShouldFilterPassedResults() {
        List<CheckResult> results = List.of(
                new CheckResult("CKV_001", "Check 1", "PASSED", "HIGH", "/file.yaml", 1, 5, null),
                new CheckResult("CKV_002", "Check 2", "PASSED", "LOW", "/file.yaml", 10, 15, null)
        );

        List<Diagnostic> diagnostics = mapper.map(results);
        assertTrue(diagnostics.isEmpty());
    }

    @Test
    void mapShouldIncludeOnlyFailedResults() {
        List<CheckResult> results = List.of(
                new CheckResult("CKV_001", "Check 1", "FAILED", "HIGH", "/file.yaml", 1, 5, null),
                new CheckResult("CKV_002", "Check 2", "PASSED", "LOW", "/file.yaml", 10, 15, null)
        );

        List<Diagnostic> diagnostics = mapper.map(results);
        assertEquals(1, diagnostics.size());
        assertEquals("CKV_001", diagnostics.get(0).code());
    }

    @Test
    void mapSeverityShouldReturnErrorForCritical() {
        assertEquals(Severity.ERROR, mapper.mapSeverity("CRITICAL"));
    }

    @Test
    void mapSeverityShouldReturnErrorForHigh() {
        assertEquals(Severity.ERROR, mapper.mapSeverity("HIGH"));
    }

    @Test
    void mapSeverityShouldReturnWarnForMedium() {
        assertEquals(Severity.WARN, mapper.mapSeverity("MEDIUM"));
    }

    @Test
    void mapSeverityShouldReturnInfoForLow() {
        assertEquals(Severity.INFO, mapper.mapSeverity("LOW"));
    }

    @Test
    void mapSeverityShouldReturnWarnForNull() {
        assertEquals(Severity.WARN, mapper.mapSeverity(null));
    }

    @Test
    void mapSeverityShouldReturnWarnForUnknown() {
        assertEquals(Severity.WARN, mapper.mapSeverity("UNKNOWN"));
    }

    @Test
    void mapSeverityShouldBeCaseInsensitive() {
        assertEquals(Severity.ERROR, mapper.mapSeverity("critical"));
        assertEquals(Severity.ERROR, mapper.mapSeverity("high"));
        assertEquals(Severity.WARN, mapper.mapSeverity("medium"));
        assertEquals(Severity.INFO, mapper.mapSeverity("low"));
    }

    @Test
    void buildMessageShouldIncludeCheckName() {
        CheckResult result = new CheckResult("CKV_001", "Ensure encryption", "FAILED",
                "HIGH", "/file.yaml", 1, 5, null);
        assertEquals("Ensure encryption", mapper.buildMessage(result));
    }

    @Test
    void buildMessageShouldIncludeGuidelineUrl() {
        CheckResult result = new CheckResult("CKV_001", "Ensure encryption", "FAILED",
                "HIGH", "/file.yaml", 1, 5, "https://docs.example.com/CKV_001");
        assertEquals("Ensure encryption (https://docs.example.com/CKV_001)", mapper.buildMessage(result));
    }

    @Test
    void buildMessageShouldIgnoreBlankGuidelineUrl() {
        CheckResult result = new CheckResult("CKV_001", "Ensure encryption", "FAILED",
                "HIGH", "/file.yaml", 1, 5, "  ");
        assertEquals("Ensure encryption", mapper.buildMessage(result));
    }

    @Test
    void toDiagnosticShouldIncludeSourceRangeWhenLineProvided() {
        CheckResult result = new CheckResult("CKV_001", "Check", "FAILED",
                "HIGH", "/file.yaml", 3, 7, null);

        Diagnostic diag = mapper.toDiagnostic(result);
        assertNotNull(diag.range());
        assertEquals(3, diag.range().startLine());
        assertEquals(7, diag.range().endLine());
    }

    @Test
    void toDiagnosticShouldHaveNullRangeWhenNoLine() {
        CheckResult result = new CheckResult("CKV_001", "Check", "FAILED",
                "HIGH", "/file.yaml", 0, 0, null);

        Diagnostic diag = mapper.toDiagnostic(result);
        assertNull(diag.range());
    }

    @Test
    void mapShouldReturnEmptyListForEmptyInput() {
        List<Diagnostic> diagnostics = mapper.map(List.of());
        assertTrue(diagnostics.isEmpty());
    }
}
