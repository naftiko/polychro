package io.polychro.checkov;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CheckovRunnerProcessTest {

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
        Path scriptFile = tempDir.resolve("fake-checkov.bat");
        Files.writeString(scriptFile, "@echo off\r\necho {\"results\":{\"passed_checks\":[],\"failed_checks\":[]}}\r\n");

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
        Path scriptFile = tempDir.resolve("failing-checkov.bat");
        Files.writeString(scriptFile, "@echo off\r\necho {\"results\":{\"passed_checks\":[],\"failed_checks\":[]}}\r\nexit /b 1\r\n");

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

        // Override startProcess to return a process that takes longer than timeout
        CheckovRunner runner = new CheckovRunner("checkov", 1, List.of(), null) {
            @Override
            Process startProcess(List<String> command) throws IOException {
                return new ProcessBuilder("cmd.exe", "/c", "ping -n 30 127.0.0.1 >nul").start();
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

        CheckovRunner runner = new CheckovRunner("checkov", 60, List.of(), null) {
            @Override
            Process startProcess(List<String> command) throws IOException {
                // Start a quick process, then interrupt the thread
                Process proc = new ProcessBuilder("cmd.exe", "/c", "echo hi").start();
                Thread.currentThread().interrupt();
                return proc;
            }
        };

        CheckovRunner.CheckovExecutionResult result = runner.run(yamlFile, CheckovFramework.YAML);

        assertFalse(result.isSuccess());
        assertNotNull(result.error());
        assertTrue(result.error().contains("interrupted"));
        // Clear the interrupted flag
        Thread.interrupted();
    }

    @Test
    void startProcessShouldReturnRunningProcess() throws IOException {
        CheckovRunner runner = new CheckovRunner("cmd.exe", 5, List.of(), null);
        Process proc = runner.startProcess(List.of("cmd.exe", "/c", "echo hello"));
        assertNotNull(proc);
        proc.destroyForcibly();
    }
}
