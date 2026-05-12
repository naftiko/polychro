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
package io.polychro.checkov;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CheckovRunnerProcessTest {

    private static final boolean IS_WINDOWS = System.getProperty("os.name", "").toLowerCase().contains("windows");

    /** Creates a minimal executable script for the current OS that prints the given JSON and exits with exitCode. */
    private static Path createScript(Path dir, String name, String jsonOutput, int exitCode) throws IOException {
        if (IS_WINDOWS) {
            Path bat = dir.resolve(name + ".bat");
            Files.writeString(bat, "@echo off\r\necho " + jsonOutput + "\r\nexit /b " + exitCode + "\r\n");
            return bat;
        } else {
            Path sh = dir.resolve(name + ".sh");
            Files.writeString(sh, "#!/bin/sh\necho '" + jsonOutput + "'\nexit " + exitCode + "\n");
            Files.setPosixFilePermissions(sh, PosixFilePermissions.fromString("rwxr-xr-x"));
            return sh;
        }
    }

    /** Returns a shell command+args that sleeps/busy-waits for ~30s (for timeout tests). */
    private static List<String> longRunningCommand() {
        return IS_WINDOWS
                ? List.of("cmd.exe", "/c", "ping -n 30 127.0.0.1 >nul")
                : List.of("sh", "-c", "sleep 30");
    }

    /** Returns a shell command+args that prints "hi" and exits immediately. */
    private static List<String> quickCommand() {
        return IS_WINDOWS
                ? List.of("cmd.exe", "/c", "echo hi")
                : List.of("sh", "-c", "echo hi");
    }

    /** Returns the shell binary name for the current OS. */
    private static String shellBinary() {
        return IS_WINDOWS ? "cmd.exe" : "sh";
    }

    @TempDir
    Path tempDir;

    @Test
    void executionResultShouldBeSuccessWhenNoError() {
        var result = new CheckovRunner.CheckovExecutionResult("{}", null, 0);
        assertTrue(result.isSuccess());
        assertEquals("{}", result.output());
        assertNull(result.error());
        assertEquals(0, result.exitCode());
    }

    @Test
    void executionResultShouldNotBeSuccessWhenErrorPresent() {
        var result = new CheckovRunner.CheckovExecutionResult(null, "some error", 1);
        assertFalse(result.isSuccess());
    }

    @Test
    void runShouldHandleIOException() throws IOException {
        Path yamlFile = tempDir.resolve("test.yaml");
        Files.writeString(yamlFile, "key: value\n");

        CheckovRunner runner = new CheckovRunner("totally-nonexistent-binary-12345", 5, List.of(), null);
        CheckovRunner.CheckovExecutionResult result = runner.run(yamlFile, CheckovFramework.YAML);

        assertFalse(result.isSuccess());
        assertNotNull(result.error());
        assertTrue(result.error().contains("not available"));
    }

    @Test
    void runShouldCaptureOutputFromSuccessfulProcess() throws IOException {
        String json = "{\"results\":{\"passed_checks\":[],\"failed_checks\":[]}}";
        Path scriptFile = createScript(tempDir, "fake-checkov", json, 0);
        Path yamlFile = tempDir.resolve("test.yaml");
        Files.writeString(yamlFile, "name: test\n");

        CheckovRunner runner = new CheckovRunner(scriptFile.toString(), 10, List.of(), null);
        CheckovRunner.CheckovExecutionResult result = runner.run(yamlFile, CheckovFramework.YAML);

        assertTrue(result.isSuccess());
        assertNotNull(result.output());
        assertNull(result.error());
    }

    @Test
    void runShouldHandleNonZeroExitCode() throws IOException {
        String json = "{\"results\":{\"passed_checks\":[],\"failed_checks\":[]}}";
        Path scriptFile = createScript(tempDir, "failing-checkov", json, 1);
        Path yamlFile = tempDir.resolve("test.yaml");
        Files.writeString(yamlFile, "name: test\n");

        CheckovRunner runner = new CheckovRunner(scriptFile.toString(), 10, List.of(), null);
        CheckovRunner.CheckovExecutionResult result = runner.run(yamlFile, CheckovFramework.YAML);

        assertTrue(result.isSuccess());
        assertEquals(1, result.exitCode());
    }

    @Test
    void runShouldTimeoutForLongRunningProcess() throws IOException {
        Path yamlFile = tempDir.resolve("test.yaml");
        Files.writeString(yamlFile, "name: test\n");

        List<String> longCmd = longRunningCommand();
        CheckovRunner runner = new CheckovRunner("checkov", 1, List.of(), null) {
            @Override
            Process startProcess(List<String> command) throws IOException {
                return new ProcessBuilder(longCmd).start();
            }
        };

        CheckovRunner.CheckovExecutionResult result = runner.run(yamlFile, CheckovFramework.YAML);

        assertFalse(result.isSuccess());
        assertNotNull(result.error());
        assertTrue(result.error().contains("timed out"));
    }

    @Test
    void runShouldHandleInterruptedException() throws IOException {
        Path yamlFile = tempDir.resolve("test.yaml");
        Files.writeString(yamlFile, "name: test\n");

        List<String> quickCmd = quickCommand();
        CheckovRunner runner = new CheckovRunner("checkov", 60, List.of(), null) {
            @Override
            Process startProcess(List<String> command) throws IOException {
                Process proc = new ProcessBuilder(quickCmd).start();
                Thread.currentThread().interrupt();
                return proc;
            }
        };

        CheckovRunner.CheckovExecutionResult result = runner.run(yamlFile, CheckovFramework.YAML);

        assertFalse(result.isSuccess());
        assertNotNull(result.error());
        assertTrue(result.error().contains("interrupted"));
        Thread.interrupted();
    }

    @Test
    void startProcessShouldReturnRunningProcess() throws IOException {
        List<String> cmd = quickCommand();
        CheckovRunner runner = new CheckovRunner(shellBinary(), 5, List.of(), null);
        Process proc = runner.startProcess(cmd);
        assertNotNull(proc);
        proc.destroyForcibly();
    }
}
