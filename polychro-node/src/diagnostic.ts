// Copyright 2026 Naftiko — Apache License 2.0

/**
 * Diagnostic types for Polychro lint results.
 */

export enum Severity {
  ERROR = "error",
  WARN = "warn",
  INFO = "info",
  HINT = "hint",
}

export interface SourceRange {
  start: { line: number; character: number };
  end: { line: number; character: number };
}

export interface Diagnostic {
  severity: Severity;
  code: string;
  message: string;
  path: string;
  range?: SourceRange;
  suggestion?: string;
}

/**
 * Format a diagnostic for LLM consumption (agent feedback loop).
 */
export function toAgentFormat(diagnostic: Diagnostic): string {
  const lines = [
    `[${diagnostic.severity.toUpperCase()}] ${diagnostic.code}`,
    `  Path: ${diagnostic.path}`,
    `  Message: ${diagnostic.message}`,
  ];
  if (diagnostic.range) {
    lines.push(
      `  Location: line ${diagnostic.range.start.line}:${diagnostic.range.start.character}`
    );
  }
  if (diagnostic.suggestion) {
    lines.push(`  Fix: ${diagnostic.suggestion}`);
  }
  return lines.join("\n");
}
