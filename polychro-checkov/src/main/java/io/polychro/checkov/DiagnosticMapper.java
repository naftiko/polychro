package io.polychro.checkov;

import io.polychro.spi.Diagnostic;
import io.polychro.spi.Severity;
import io.polychro.spi.SourceRange;

import java.util.List;

/**
 * Maps Checkov CheckResult objects to Polychro Diagnostic records.
 */
class DiagnosticMapper {

    List<Diagnostic> map(List<CheckResult> results) {
        return results.stream()
                .filter(r -> "FAILED".equals(r.result()))
                .map(this::toDiagnostic)
                .toList();
    }

    Diagnostic toDiagnostic(CheckResult result) {
        Severity severity = mapSeverity(result.severity());
        String message = buildMessage(result);
        SourceRange range = null;
        if (result.startLine() > 0) {
            range = new SourceRange(result.startLine(), 1, result.endLine(), 1);
        }
        return new Diagnostic(severity, result.checkId(), message, result.filePath(), range);
    }

    Severity mapSeverity(String checkovSeverity) {
        if (checkovSeverity == null) {
            return Severity.WARN;
        }
        return switch (checkovSeverity.toUpperCase()) {
            case "CRITICAL", "HIGH" -> Severity.ERROR;
            case "MEDIUM" -> Severity.WARN;
            case "LOW" -> Severity.INFO;
            default -> Severity.WARN;
        };
    }

    String buildMessage(CheckResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append(result.checkName());
        if (result.guidelineUrl() != null && !result.guidelineUrl().isBlank()) {
            sb.append(" (").append(result.guidelineUrl()).append(")");
        }
        return sb.toString();
    }
}
