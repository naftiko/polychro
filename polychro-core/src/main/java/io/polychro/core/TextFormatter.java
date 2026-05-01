package io.polychro.core;

import io.polychro.spi.Diagnostic;
import io.polychro.spi.Severity;

import java.util.List;

/**
 * Formats diagnostics as human-readable text.
 */
public class TextFormatter implements DiagnosticFormatter {

    @Override
    public String format(List<Diagnostic> diagnostics) {
        if (diagnostics == null || diagnostics.isEmpty()) {
            return "No issues found.\n";
        }

        StringBuilder sb = new StringBuilder();
        for (Diagnostic d : diagnostics) {
            sb.append(formatSeverity(d.severity()));
            if (d.path() != null) {
                sb.append(" at ").append(d.path());
            }
            if (d.code() != null) {
                sb.append(" [").append(d.code()).append("]");
            }
            sb.append(": ").append(d.message());
            sb.append('\n');
        }
        sb.append('\n');
        sb.append(diagnostics.size()).append(" issue(s) found.\n");
        return sb.toString();
    }

    private String formatSeverity(Severity severity) {
        return switch (severity) {
            case ERROR -> "ERROR";
            case WARN -> "WARN";
            case INFO -> "INFO";
            case HINT -> "HINT";
        };
    }
}
