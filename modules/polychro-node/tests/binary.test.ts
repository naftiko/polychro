// Copyright 2026 Naftiko — Apache License 2.0

import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";

vi.mock("node:child_process", () => ({
  execSync: vi.fn(() => ""),
}));

vi.mock("node:fs", () => ({
  existsSync: vi.fn(() => false),
}));

import { execSync } from "node:child_process";
import { existsSync } from "node:fs";
import { commandFor, resolveBinary } from "../src/binary.js";

const WRAPPER = "C:\\Users\\me\\.polychro\\bin\\polychro.cmd";
const CURRENT = "C:\\Users\\me\\.polychro\\current";
const JAVA = `${CURRENT}\\jre\\bin\\java.exe`;
const JAR = `${CURRENT}\\polychro.jar`;

describe("commandFor", () => {
  const installed = (path: string) => path === JAVA || path === JAR;

  it("should run the binary itself when it is not a batch file", () => {
    const command = commandFor("/usr/local/bin/polychro", ["lint", "a.yml"], "linux", {}, installed);
    expect(command).toEqual({ file: "/usr/local/bin/polychro", args: ["lint", "a.yml"] });
  });

  it("should run polychro.exe itself on Windows", () => {
    const command = commandFor("C:\\tools\\polychro.exe", ["lint", "a.yml"], "win32", {}, installed);
    expect(command).toEqual({ file: "C:\\tools\\polychro.exe", args: ["lint", "a.yml"] });
  });

  it("should run the bundled java when the binary is the polychro.cmd wrapper", () => {
    const command = commandFor(WRAPPER, ["lint", "a & b.yml"], "win32", {}, installed);
    expect(command).toEqual({
      file: JAVA,
      args: [
        "-XX:+VerifySharedSpaces",
        `-XX:SharedArchiveFile=${CURRENT}\\polychro.jsa`,
        "-Xlog:cds*=off",
        "-jar",
        JAR,
        "lint",
        "a & b.yml",
      ],
    });
  });

  it("should match the wrapper extension in any case", () => {
    const command = commandFor(WRAPPER.toUpperCase(), ["--version"], "win32", {}, (path) =>
      path.toUpperCase() === JAVA.toUpperCase() || path.toUpperCase() === JAR.toUpperCase()
    );
    expect(command.file.toUpperCase()).toBe(JAVA.toUpperCase());
  });

  it("should use POLYCHRO_HOME when it is set, like the wrapper", () => {
    const home = "D:\\polychro home";
    const exists = (path: string) => path.startsWith(`${home}\\current\\`);
    const command = commandFor(WRAPPER, ["--version"], "win32", { POLYCHRO_HOME: home }, exists);
    expect(command.file).toBe(`${home}\\current\\jre\\bin\\java.exe`);
    expect(command.args).toContain(`${home}\\current\\polychro.jar`);
  });

  it("should fail with a clear message when no install is next to the batch file", () => {
    expect(() => commandFor("C:\\other\\polychro.bat", ["--version"], "win32", {}, () => false)).toThrow(
      /Node\.js cannot run without a shell.*POLYCHRO_BIN/
    );
  });
});

describe("resolveBinary", () => {
  let savedBin: string | undefined;

  beforeEach(() => {
    vi.clearAllMocks();
    savedBin = process.env["POLYCHRO_BIN"];
    delete process.env["POLYCHRO_BIN"];
  });

  afterEach(() => {
    if (savedBin !== undefined) process.env["POLYCHRO_BIN"] = savedBin;
  });

  it("should take the first match when `where` lists several with Windows line endings", () => {
    vi.mocked(execSync).mockReturnValue(`${WRAPPER}\r\nC:\\tools\\polychro.exe\r\n`);
    vi.mocked(existsSync).mockImplementation((path) => path === WRAPPER);
    expect(resolveBinary()).toBe(WRAPPER);
  });
});
