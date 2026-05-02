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
