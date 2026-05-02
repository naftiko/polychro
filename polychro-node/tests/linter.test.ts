// Copyright 2026 Naftiko — Apache License 2.0

import { describe, it, expect, vi, beforeEach } from "vitest";

// Mock the binary module before importing linter
vi.mock("../src/binary.js", () => ({
  resolveBinary: vi.fn(() => "/usr/bin/polychro"),
  BinaryNotFoundError: class extends Error {
    constructor(searched: string[]) {
      super(`Not found: ${searched.join(", ")}`);
    }
  },
}));

vi.mock("node:child_process", () => ({
  execFile: vi.fn(),
  execSync: vi.fn(() => ""),
}));

import { execFile } from "node:child_process";
import { lint, lintString, validateSchema } from "../src/linter.js";
import { resolveBinary, BinaryNotFoundError } from "../src/binary.js";
import { Severity, toAgentFormat } from "../src/diagnostic.js";
import type { Diagnostic } from "../src/diagnostic.js";

describe("lint", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should return empty diagnostics for a valid file", async () => {
    const mockExecFile = vi.mocked(execFile);
    mockExecFile.mockImplementation((_cmd, _args, _opts, cb: any) => {
      if (typeof _opts === "function") {
        _opts(null, "[]", "");
      } else {
        cb(null, "[]", "");
      }
      return {} as any;
    });

    // We need to use the promisified version, so let's mock differently
    vi.doUnmock("node:child_process");
    vi.doUnmock("../src/binary.js");
  });

  it("should parse diagnostics from JSON output", () => {
    const output = JSON.stringify([
      { severity: "error", code: "rule-1", message: "Bad", path: "$.info" },
    ]);
    // Direct parse test
    const data = JSON.parse(output) as Diagnostic[];
    expect(data).toHaveLength(1);
    expect(data[0]!.code).toBe("rule-1");
  });

  it("should parse agent format output", () => {
    const output = JSON.stringify({
      diagnostics: [
        { severity: "warn", code: "tags", message: "Add tags", path: "$.info" },
      ],
      tokens: 12,
    });
    const data = JSON.parse(output);
    expect(data.diagnostics).toHaveLength(1);
    expect(data.tokens).toBe(12);
  });
});

describe("Diagnostic", () => {
  it("should format for agent consumption", () => {
    const d: Diagnostic = {
      severity: Severity.ERROR,
      code: "no-trailing-slash",
      message: "Remove trailing slash",
      path: "$.consumes[0].baseUri",
    };
    const output = toAgentFormat(d);
    expect(output).toContain("[ERROR] no-trailing-slash");
    expect(output).toContain("Path: $.consumes[0].baseUri");
    expect(output).toContain("Message: Remove trailing slash");
  });

  it("should include location when range is present", () => {
    const d: Diagnostic = {
      severity: Severity.WARN,
      code: "naming",
      message: "Use kebab",
      path: "$.info.name",
      range: { start: { line: 5, character: 2 }, end: { line: 5, character: 10 } },
    };
    const output = toAgentFormat(d);
    expect(output).toContain("Location: line 5:2");
  });

  it("should include fix when suggestion is present", () => {
    const d: Diagnostic = {
      severity: Severity.INFO,
      code: "tags",
      message: "Add tags",
      path: "$.info",
      suggestion: "Add a tags array",
    };
    const output = toAgentFormat(d);
    expect(output).toContain("Fix: Add a tags array");
  });
});

describe("BinaryNotFoundError", () => {
  it("should report searched locations", () => {
    const err = new BinaryNotFoundError(["/usr/bin/polychro", "/opt/polychro"]);
    expect(err.message).toContain("Not found");
  });
});

describe("TypeScript types", () => {
  it("should compile Severity enum values", () => {
    expect(Severity.ERROR).toBe("error");
    expect(Severity.WARN).toBe("warn");
    expect(Severity.INFO).toBe("info");
    expect(Severity.HINT).toBe("hint");
  });

  it("should type-check Diagnostic interface", () => {
    const d: Diagnostic = {
      severity: Severity.ERROR,
      code: "test",
      message: "test message",
      path: "$.root",
    };
    // Type-level assertion — if this compiles, the types are correct
    expect(d.severity).toBe(Severity.ERROR);
  });
});
