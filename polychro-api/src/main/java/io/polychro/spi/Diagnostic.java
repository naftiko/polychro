package io.polychro.spi;

/**
 * A single diagnostic produced by a validator.
 * <p>
 * Diagnostics are sorted by severity (descending — ERROR first) then by path (ascending).
 *
 * @param severity the diagnostic severity level
 * @param code     a short machine-readable code (e.g. rule name), may be null
 * @param message  human-readable description of the issue
 * @param path     JSONPath or document path where the issue was found, may be null
 * @param range    source location within the document, may be null
 */
public record Diagnostic(
        Severity severity,
        String code,
        String message,
        String path,
        SourceRange range
) implements Comparable<Diagnostic> {

    @Override
    public int compareTo(Diagnostic other) {
        // Severity descending (ERROR < WARN < INFO < HINT in ordinal, so lower ordinal = higher priority)
        int severityCompare = Integer.compare(this.severity.ordinal(), other.severity.ordinal());
        if (severityCompare != 0) {
            return severityCompare;
        }
        // Path ascending, nulls last
        if (this.path == null && other.path == null) {
            return 0;
        }
        if (this.path == null) {
            return 1;
        }
        if (other.path == null) {
            return -1;
        }
        return this.path.compareTo(other.path);
    }
}
