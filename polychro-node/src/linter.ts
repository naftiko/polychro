// Copyright 2026 Naftiko — Apache License 2.0

/**
 * Core linting interface — subprocess wrapper around the Polychro native binary.
 */

import { execFile } from "node:child_process";
import { writeFile, unlink } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { randomUUID } from "node:crypto";
import { promisify } from "node:util";
import { resolveBinary } from "./binary.js";
import type { Diagnostic } from "./diagnostic.js";

const execFileAsync = promisify(execFile);

export interface LinterOptions {
  /** Path to a custom ruleset file. */
  ruleset?: string;
  /** Path to a custom schema file. */
  schema?: string;
  /** Path to .polychro.yml config file. */
  config?: string;
  /** Timeout in milliseconds (default: 30000). */
  timeout?: number;
}

export interface LintResult {
  diagnostics: Diagnostic[];
  tokens?: number;
}

function buildArgs(
  command: string,
  file: string,
  options: LinterOptions,
  extra?: string[]
): string[] {
  const args = [command, "--format", "json"];
  if (options.ruleset) args.push("--ruleset", options.ruleset);
  if (options.schema) args.push("--schema", options.schema);
  if (options.config) args.push("--config", options.config);
  if (extra) args.push(...extra);
  args.push(file);
  return args;
}

function parseLintOutput(stdout: string): LintResult {
  if (!stdout.trim()) {
    return { diagnostics: [] };
  }

  const data = JSON.parse(stdout);

  if (Array.isArray(data)) {
    return { diagnostics: data as Diagnostic[] };
  }

  // Agent format: { diagnostics: [...], tokens: N }
  return {
    diagnostics: (data.diagnostics ?? []) as Diagnostic[],
    tokens: data.tokens,
  };
}

/**
 * Lint a file and return structured diagnostics.
 */
export async function lint(
  file: string,
  options: LinterOptions = {}
): Promise<LintResult> {
  const binary = resolveBinary();
  const args = buildArgs("lint", file, options);
  const timeout = options.timeout ?? 30_000;

  try {
    const { stdout } = await execFileAsync(binary, args, { timeout });
    return parseLintOutput(stdout);
  } catch (err: unknown) {
    const execErr = err as { code?: string; stdout?: string; killed?: boolean };
    if (execErr.killed) {
      throw new Error(`Polychro binary timed out after ${timeout}ms`);
    }
    // Exit code 1 = diagnostics found (not a failure)
    if (execErr.stdout) {
      return parseLintOutput(execErr.stdout);
    }
    throw err;
  }
}

/**
 * Lint a YAML/JSON string.
 */
export async function lintString(
  content: string,
  options: LinterOptions & { format?: "yaml" | "json" } = {}
): Promise<LintResult> {
  const ext = options.format === "json" ? ".json" : ".yml";
  const tmpFile = join(tmpdir(), `polychro-${randomUUID()}${ext}`);
  await writeFile(tmpFile, content, "utf-8");
  try {
    return await lint(tmpFile, options);
  } finally {
    await unlink(tmpFile).catch(() => {});
  }
}

/**
 * Run schema validation only.
 */
export async function validateSchema(
  file: string,
  options: LinterOptions = {}
): Promise<LintResult> {
  const binary = resolveBinary();
  const args = buildArgs("lint", file, options, ["--validators", "json-schema"]);
  const timeout = options.timeout ?? 30_000;

  try {
    const { stdout } = await execFileAsync(binary, args, { timeout });
    return parseLintOutput(stdout);
  } catch (err: unknown) {
    const execErr = err as { stdout?: string; killed?: boolean };
    if (execErr.killed) {
      throw new Error(`Polychro binary timed out after ${timeout}ms`);
    }
    if (execErr.stdout) {
      return parseLintOutput(execErr.stdout);
    }
    throw err;
  }
}

/**
 * List active rules (placeholder — returns empty until CLI exposes list-rules).
 */
export async function listRules(
  _options: LinterOptions = {}
): Promise<Array<{ name: string; description: string; severity: string }>> {
  return [];
}
