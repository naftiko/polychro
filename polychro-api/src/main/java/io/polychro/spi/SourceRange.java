package io.polychro.spi;

/**
 * A 1-based line/column range within a source document.
 *
 * @param startLine   1-based start line
 * @param startColumn 1-based start column
 * @param endLine     1-based end line
 * @param endColumn   1-based end column
 */
public record SourceRange(int startLine, int startColumn, int endLine, int endColumn) {
}
