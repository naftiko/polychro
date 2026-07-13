// Copyright 2026 Naftiko — Apache License 2.0

/**
 * Polychro Node.js SDK — thin wrapper for the Polychro native binary.
 */

export { lint, lintString, validateSchema, listRules } from "./linter.js";
export type { LintResult, LinterOptions } from "./linter.js";
export type { Diagnostic, SourceRange } from "./diagnostic.js";
export { Severity } from "./diagnostic.js";
