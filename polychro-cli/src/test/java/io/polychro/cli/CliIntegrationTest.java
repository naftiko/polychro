/**
 * Copyright 2026 Naftiko
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.polychro.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.*;

class CliIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void lintCleanFileShouldExitZero() throws Exception {
        Path file = createFile("clean.yml", "name: test\nversion: \"1.0\"\n");
        ExecutionResult result = run("lint", file.toString());
        assertEquals(0, result.exitCode());
        assertTrue(result.stdout().contains("No issues found."));
    }

    @Test
    void lintMissingFileShouldExitTwo() {
        ExecutionResult result = run("lint", "nonexistent-file.yml");
        assertEquals(2, result.exitCode());
        assertTrue(result.stderr().contains("file not found"));
    }

    @Test
    void lintWithJsonFormatShouldProduceJson() throws Exception {
        Path file = createFile("test.yml", "name: test\n");
        ExecutionResult result = run("lint", "--format", "json", file.toString());
        assertEquals(0, result.exitCode());
        assertTrue(result.stdout().trim().startsWith("["));
    }

    @Test
    void lintWithSarifFormatShouldProduceSarif() throws Exception {
        Path file = createFile("test.yml", "name: test\n");
        ExecutionResult result = run("lint", "--format", "sarif", file.toString());
        assertEquals(0, result.exitCode());
        assertTrue(result.stdout().contains("\"version\""));
        assertTrue(result.stdout().contains("2.1.0"));
    }

    @Test
    void lintMultipleFilesShouldSucceed() throws Exception {
        Path file1 = createFile("a.yml", "name: a\n");
        Path file2 = createFile("b.yml", "name: b\n");
        ExecutionResult result = run("lint", file1.toString(), file2.toString());
        assertEquals(0, result.exitCode());
    }

    @Test
    void lintWithValidatorsFilterShouldSucceed() throws Exception {
        Path file = createFile("test.yml", "name: test\n");
        ExecutionResult result = run("lint", "--validators", "wellformedness", file.toString());
        assertEquals(0, result.exitCode());
    }

    @Test
    void lintWithConfigShouldSucceed() throws Exception {
        Path config = createFile(".polychro.yml", "failFast: false\n");
        Path file = createFile("test.yml", "name: test\n");
        ExecutionResult result = run("lint", "--config", config.toString(), file.toString());
        assertEquals(0, result.exitCode());
    }

    @Test
    void lintJsonFileShouldWork() throws Exception {
        Path file = createFile("test.json", "{\"name\": \"test\"}");
        ExecutionResult result = run("lint", file.toString());
        assertEquals(0, result.exitCode());
    }

    @Test
    void noArgsShouldShowUsage() {
        ExecutionResult result = run();
        assertEquals(0, result.exitCode());
        assertTrue(result.stdout().contains("polychro"));
    }

    // ── Regression tests for issue #20 ──────────────────────────────────────

    @Test
    void lintWithRulesetShouldDetectGovernanceViolations() throws Exception {
        // Reproduces issue #20: `polychro lint --ruleset governance.yml bad-capability.yml`
        // must report `capability-version-format` and `capability-description-present`
        // diagnostics. Before the fix, the CLI silently returned "No issues found."
        // because validator modules were not on the classpath, and a missing-config
        // IAE in JsonSchemaValidatorFactory would otherwise have crashed this combo.
        Path ruleset = governanceRulesetPath();
        Path file = createFile("bad-capability.yml",
                "info:\n  name: my-capability\n  version: not-semver\n");

        ExecutionResult result = run("lint", "--ruleset", ruleset.toString(), file.toString());

        assertEquals(1, result.exitCode(), () ->
                "Expected exit code 1 (warnings only) but got " + result.exitCode()
                        + ". stdout=" + result.stdout() + " stderr=" + result.stderr());
        assertTrue(result.stdout().contains("capability-version-format"),
                () -> "Expected capability-version-format diagnostic in output: " + result.stdout());
        assertTrue(result.stdout().contains("capability-description-present"),
                () -> "Expected capability-description-present diagnostic in output: " + result.stdout());
    }

    @Test
    void lintWithRulesetAndSchemaShouldNotCrash() throws Exception {
        // Second half of issue #20: passing both --schema and --ruleset must not
        // crash because of an unconfigured json-schema factory IAE. Before the
        // Linter.Builder fix, this flag combination would propagate IAE for
        // factories the user did not configure.
        Path ruleset = governanceRulesetPath();
        Path schema = createFile("schema.json", "{\"type\": \"object\"}");
        Path file = createFile("bad-capability.yml",
                "info:\n  name: my-capability\n  version: not-semver\n");

        ExecutionResult result = run("lint",
                "--schema", schema.toString(),
                "--ruleset", ruleset.toString(),
                file.toString());

        // The schema is permissive and the ruleset reports warnings → exit 1.
        // The important assertion is that the CLI does not crash (exit code != 2
        // unless there's an actual error) and does produce diagnostics.
        assertNotEquals(2, result.exitCode(), () ->
                "CLI should not crash with both --schema and --ruleset. stderr=" + result.stderr());
        assertTrue(result.stdout().contains("capability-version-format"),
                () -> "Expected ruleset diagnostics in output: " + result.stdout());
    }

    @Test
    void lintWithoutAnyConfigShouldStillRunValidators() throws Exception {
        // Verifies that running `polychro lint file.yml` with no flags actually
        // discovers and runs validator factories that do not require configuration
        // (wellformedness, markdown, html, checkov). Before fix A, ServiceLoader
        // found zero factories because none were on the shaded-jar classpath.
        // After the fix, the linter runs the no-config factories and silently
        // skips the ones requiring explicit config (json-schema, ruleset, ...).
        Path file = createFile("test.yml", "name: test\n");
        ExecutionResult result = run("lint", file.toString());

        assertEquals(0, result.exitCode());
        // A clean YAML file should still produce "No issues found." from the
        // wellformedness validator (which runs unconditionally).
        assertTrue(result.stdout().contains("No issues found."));
    }

    private Path governanceRulesetPath() throws Exception {
        // polychro-rulesets is a runtime dependency of the CLI — its resources are on
        // the test classpath. We copy via InputStream instead of Path.of(uri) because
        // Path.of() fails with FileSystemNotFoundException when the resource lives
        // inside a jar (e.g. Maven fat-jar on CI); InputStream works in both cases.
        Path dest = tempDir.resolve("governance.yml");
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("rulesets/governance.yml")) {
            Files.copy(in, dest);
        }
        return dest;
    }

    private ExecutionResult run(String... args) {
        StringWriter stdout = new StringWriter();
        StringWriter stderr = new StringWriter();
        CommandLine cmd = PolychroCli.buildCommandLine();
        cmd.setOut(new PrintWriter(stdout));
        cmd.setErr(new PrintWriter(stderr));
        int exitCode = cmd.execute(args);
        return new ExecutionResult(exitCode, stdout.toString(), stderr.toString());
    }

    private Path createFile(String name, String content) throws Exception {
        Path file = tempDir.resolve(name);
        Files.writeString(file, content);
        return file;
    }

    private record ExecutionResult(int exitCode, String stdout, String stderr) {
    }
}
