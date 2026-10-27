// Copyright 2026 Naftiko — Apache License 2.0

/**
 * Binary resolution for the Polychro native executable.
 */

import { existsSync } from "node:fs";
import { join, dirname, win32 } from "node:path";
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
      // `where` ends its lines with \r\n and lists every match.
      candidates.push(result.split(/\r?\n/)[0]!);
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

export interface Command {
  file: string;
  args: string[];
}

/**
 * The program and arguments that run `binary` with `args`.
 *
 * On Windows, scripts/install.ps1 puts a `polychro.cmd` wrapper on PATH. Node.js refuses to
 * start a `.cmd` or `.bat` file without a shell (CVE-2024-27980), and cmd.exe cannot be given
 * arbitrary file paths safely. So the wrapper's own command is run instead: the bundled
 * java.exe, with the options the wrapper passes.
 */
export function commandFor(
  binary: string,
  args: string[],
  platform: NodeJS.Platform = process.platform,
  env: NodeJS.ProcessEnv = process.env,
  fileExists: (path: string) => boolean = existsSync
): Command {
  if (platform !== "win32" || !/\.(cmd|bat)$/i.test(binary)) {
    return { file: binary, args };
  }

  // Same lookup as the wrapper: POLYCHRO_HOME if set, else the parent of the wrapper's folder.
  const home = env["POLYCHRO_HOME"] || win32.join(win32.dirname(binary), "..");
  const current = win32.join(home, "current");
  const java = win32.join(current, "jre", "bin", "java.exe");
  const jar = win32.join(current, "polychro.jar");
  if (!fileExists(java) || !fileExists(jar)) {
    throw new Error(
      `${binary} is a batch file, which Node.js cannot run without a shell, and no ` +
        `install was found next to it (${java}). Set POLYCHRO_BIN to polychro.exe, ` +
        `or reinstall Polychro with scripts/install.ps1.`
    );
  }
  return {
    file: java,
    args: [
      "-XX:+VerifySharedSpaces",
      `-XX:SharedArchiveFile=${win32.join(current, "polychro.jsa")}`,
      "-Xlog:cds*=off",
      "-jar",
      jar,
      ...args,
    ],
  };
}
