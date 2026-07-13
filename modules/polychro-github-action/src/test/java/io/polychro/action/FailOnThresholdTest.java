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
package io.polychro.action;

import io.polychro.spi.Diagnostic;
import io.polychro.spi.Severity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FailOnThresholdTest {

    private static Diagnostic diag(Severity severity) {
        return new Diagnostic(severity, "test-rule", "message", "$.path", null);
    }

    @Test
    void computeExitCodeShouldReturn0ForEmptyDiagnostics() {
        assertEquals(0, FailOnThreshold.computeExitCode(List.of(), "error"));
    }

    @Test
    void computeExitCodeShouldReturn0ForNull() {
        assertEquals(0, FailOnThreshold.computeExitCode(null, "error"));
    }

    @Test
    void computeExitCodeShouldReturn1WhenErrorMeetsErrorThreshold() {
        List<Diagnostic> diagnostics = List.of(diag(Severity.ERROR));
        assertEquals(1, FailOnThreshold.computeExitCode(diagnostics, "error"));
    }

    @Test
    void computeExitCodeShouldReturn0WhenWarnDoesNotMeetErrorThreshold() {
        List<Diagnostic> diagnostics = List.of(diag(Severity.WARN));
        assertEquals(0, FailOnThreshold.computeExitCode(diagnostics, "error"));
    }

    @Test
    void computeExitCodeShouldReturn1WhenWarnMeetsWarnThreshold() {
        List<Diagnostic> diagnostics = List.of(diag(Severity.WARN));
        assertEquals(1, FailOnThreshold.computeExitCode(diagnostics, "warn"));
    }

    @Test
    void computeExitCodeShouldReturn1WhenErrorMeetsWarnThreshold() {
        List<Diagnostic> diagnostics = List.of(diag(Severity.ERROR));
        assertEquals(1, FailOnThreshold.computeExitCode(diagnostics, "warn"));
    }

    @Test
    void computeExitCodeShouldReturn0WhenInfoDoesNotMeetWarnThreshold() {
        List<Diagnostic> diagnostics = List.of(diag(Severity.INFO));
        assertEquals(0, FailOnThreshold.computeExitCode(diagnostics, "warn"));
    }

    @Test
    void computeExitCodeShouldReturn1WhenInfoMeetsInfoThreshold() {
        List<Diagnostic> diagnostics = List.of(diag(Severity.INFO));
        assertEquals(1, FailOnThreshold.computeExitCode(diagnostics, "info"));
    }

    @Test
    void computeExitCodeShouldDefaultToErrorWhenThresholdNull() {
        List<Diagnostic> diagnostics = List.of(diag(Severity.WARN));
        assertEquals(0, FailOnThreshold.computeExitCode(diagnostics, null));
    }

    @Test
    void computeExitCodeShouldDefaultToErrorWhenThresholdBlank() {
        List<Diagnostic> diagnostics = List.of(diag(Severity.ERROR));
        assertEquals(1, FailOnThreshold.computeExitCode(diagnostics, "  "));
    }

    @Test
    void computeExitCodeShouldAcceptWarningAlias() {
        List<Diagnostic> diagnostics = List.of(diag(Severity.WARN));
        assertEquals(1, FailOnThreshold.computeExitCode(diagnostics, "warning"));
    }

    @Test
    void formatSummaryShouldReturnNoIssuesForEmpty() {
        assertEquals("No issues found.", FailOnThreshold.formatSummary(List.of()));
    }

    @Test
    void formatSummaryShouldReturnNoIssuesForNull() {
        assertEquals("No issues found.", FailOnThreshold.formatSummary(null));
    }

    @Test
    void formatSummaryShouldIncludeErrorCount() {
        List<Diagnostic> diagnostics = List.of(diag(Severity.ERROR), diag(Severity.ERROR));
        String summary = FailOnThreshold.formatSummary(diagnostics);
        assertTrue(summary.contains("2 issue(s) found"));
        assertTrue(summary.contains("2 error(s)"));
    }

    @Test
    void formatSummaryShouldIncludeMixedCounts() {
        List<Diagnostic> diagnostics = List.of(
                diag(Severity.ERROR), diag(Severity.WARN), diag(Severity.INFO));
        String summary = FailOnThreshold.formatSummary(diagnostics);
        assertTrue(summary.contains("3 issue(s) found"));
        assertTrue(summary.contains("1 error(s)"));
        assertTrue(summary.contains("1 warning(s)"));
        assertTrue(summary.contains("1 info"));
    }

    @Test
    void cacheKeyShouldCombineVersionAndPlatform() {
        assertEquals("polychro-0.1.0-linux-x64", FailOnThreshold.cacheKey("0.1.0", "linux-x64"));
    }

    @Test
    void cacheKeyShouldHandleLatest() {
        assertEquals("polychro-latest-darwin-arm64", FailOnThreshold.cacheKey("latest", "darwin-arm64"));
    }

    @Test
    void parseThresholdShouldHandleHint() {
        List<Diagnostic> diagnostics = List.of(diag(Severity.HINT));
        assertEquals(1, FailOnThreshold.computeExitCode(diagnostics, "hint"));
    }

    @Test
    void parseThresholdShouldDefaultUnknownToError() {
        List<Diagnostic> diagnostics = List.of(diag(Severity.WARN));
        assertEquals(0, FailOnThreshold.computeExitCode(diagnostics, "unknown"));
    }

    @Test
    void formatSummaryShouldIncludeHintCount() {
        List<Diagnostic> diagnostics = List.of(diag(Severity.HINT));
        String summary = FailOnThreshold.formatSummary(diagnostics);
        assertTrue(summary.contains("1 issue(s) found"));
        assertTrue(summary.contains("1 hint(s)"));
    }
}
