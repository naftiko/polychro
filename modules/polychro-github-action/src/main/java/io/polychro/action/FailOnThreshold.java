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

import java.util.List;

/**
 * Determines the exit code based on a configurable severity threshold.
 * <p>
 * When {@code --fail-on} is set to a severity level, the action fails (exit 1)
 * if any diagnostic meets or exceeds that threshold.
 */
public final class FailOnThreshold {

    private FailOnThreshold() {
    }

    /**
     * Compute the exit code based on diagnostics and the fail-on threshold.
     *
     * @param diagnostics the lint results
     * @param threshold   the minimum severity to trigger failure ("error", "warn", "info")
     * @return 0 if no diagnostic meets the threshold, 1 otherwise
     */
    public static int computeExitCode(List<Diagnostic> diagnostics, String threshold) {
        if (diagnostics == null || diagnostics.isEmpty()) {
            return 0;
        }

        Severity minSeverity = parseThreshold(threshold);

        boolean exceeds = diagnostics.stream()
                .anyMatch(d -> meetsThreshold(d.severity(), minSeverity));

        return exceeds ? 1 : 0;
    }

    /**
     * Format a summary line for PR comments.
     *
     * @param diagnostics the lint results
     * @return a markdown summary string with counts by severity
     */
    public static String formatSummary(List<Diagnostic> diagnostics) {
        if (diagnostics == null || diagnostics.isEmpty()) {
            return "No issues found.";
        }

        long errors = diagnostics.stream().filter(d -> d.severity() == Severity.ERROR).count();
        long warnings = diagnostics.stream().filter(d -> d.severity() == Severity.WARN).count();
        long infos = diagnostics.stream().filter(d -> d.severity() == Severity.INFO).count();
        long hints = diagnostics.stream().filter(d -> d.severity() == Severity.HINT).count();

        StringBuilder sb = new StringBuilder();
        sb.append("**Polychro Lint Results:** ");
        sb.append(diagnostics.size()).append(" issue(s) found");

        List<String> parts = new java.util.ArrayList<>();
        if (errors > 0) parts.add(errors + " error(s)");
        if (warnings > 0) parts.add(warnings + " warning(s)");
        if (infos > 0) parts.add(infos + " info");
        if (hints > 0) parts.add(hints + " hint(s)");

        if (!parts.isEmpty()) {
            sb.append(" — ").append(String.join(", ", parts));
        }

        return sb.toString();
    }

    /**
     * Compute a cache key for binary caching in GitHub Actions.
     *
     * @param version  the Polychro version
     * @param platform the target platform (e.g. "linux-x64")
     * @return a deterministic cache key
     */
    public static String cacheKey(String version, String platform) {
        return "polychro-" + version + "-" + platform;
    }

    static Severity parseThreshold(String threshold) {
        if (threshold == null || threshold.isBlank()) {
            return Severity.ERROR;
        }
        return switch (threshold.strip().toLowerCase()) {
            case "warn", "warning" -> Severity.WARN;
            case "info" -> Severity.INFO;
            case "hint" -> Severity.HINT;
            default -> Severity.ERROR;
        };
    }

    static boolean meetsThreshold(Severity actual, Severity threshold) {
        // Severity ordinal: ERROR=0, WARN=1, INFO=2, HINT=3
        // "meets threshold" means actual is at least as severe as threshold
        return actual.ordinal() <= threshold.ordinal();
    }
}
