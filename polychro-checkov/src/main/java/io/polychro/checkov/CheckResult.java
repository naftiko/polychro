package io.polychro.checkov;

/**
 * A single check result from Checkov JSON output.
 */
record CheckResult(
        String checkId,
        String checkName,
        String result,       // "PASSED" or "FAILED"
        String severity,     // "CRITICAL", "HIGH", "MEDIUM", "LOW", or null
        String filePath,
        int startLine,
        int endLine,
        String guidelineUrl
) {
}
