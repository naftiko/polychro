// Copyright 2026 Naftiko — Apache License 2.0

/**
 * Binary resolution for the Polychro native executable.
 */

import { existsSync } from "node:fs";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";
import { execSync } from "node:child_process";

const __dirname = dirname(fileURLToPath(import.meta.url));

export class BinaryNotFoundError extends Error {
  constructor(searched: string[]) {
    super(
      `Polychro binary not found. Install it or set POLYCHRO_BIN. Searched: ${searched.join(", ")}`
    );
    this.name = "BinaryNotFoundError";
  }
}

function platformBinaryName(): string {
  return process.platform === "win32" ? "polychro.exe" : "polychro";
}

function searchPaths(): string[] {
  const candidates: string[] = [];

  // 1. POLYCHRO_BIN environment variable
  const envBin = process.env["POLYCHRO_BIN"];
  if (envBin) {
    candidates.push(envBin);
  }

  // 2. Adjacent bin directory (bundled distribution)
  candidates.push(join(__dirname, "..", "bin", platformBinaryName()));

  // 3. On PATH
  try {
    const cmd = process.platform === "win32" ? "where polychro" : "which polychro";
    const result = execSync(cmd, { encoding: "utf-8" }).trim();
    if (result) {
      candidates.push(result.split("\n")[0]!);
    }
  } catch {
    // not on PATH
  }

  return candidates;
}

export function resolveBinary(): string {
  const candidates = searchPaths();
  for (const candidate of candidates) {
    if (existsSync(candidate)) {
      return candidate;
    }
  }
  throw new BinaryNotFoundError(candidates);
}
