package io.polychro.core;

import io.polychro.spi.Diagnostic;

import java.util.List;

/**
 * Formats diagnostics into a specific output representation.
 */
public interface DiagnosticFormatter {

    /**
     * Format a list of diagnostics into a string.
     *
     * @param diagnostics the diagnostics to format
     * @return the formatted output string
     */
    String format(List<Diagnostic> diagnostics);
}
