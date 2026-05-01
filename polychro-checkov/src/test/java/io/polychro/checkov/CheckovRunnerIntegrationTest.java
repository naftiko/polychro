package io.polychro.checkov;

import io.polychro.spi.Diagnostic;
import io.polychro.spi.Severity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CheckovRunnerIntegrationTest {

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
    void runShouldHandleProcessIOException() throws IOException {
        Path yamlFile = tempDir.resolve("test.yaml");
        Files.writeString(yamlFile, "key: value\n");

        // Use a path that will cause IOException (non-existent binary)
        CheckovRunner runner = new CheckovRunner("totally-nonexistent-binary-that-cannot-exist-12345", 5, List.of(), null);
        CheckovRunner.CheckovExecutionResult result = runner.run(yamlFile, CheckovFramework.YAML);

        assertFalse(result.isSuccess());
        assertNotNull(result.error());
        assertTrue(result.error().contains("not available"));
    }

    @Test
    void validateShouldReturnExecutionErrorForUnexpectedErrorMessage() throws IOException {
        Path yamlFile = tempDir.resolve("test.yaml");
        Files.writeString(yamlFile, "name: test\n");

        // Create a validator with a runner that returns an error without the expected keywords
        CheckovRunner runner = new CheckovRunner("checkov", 60, List.of(), null) {
            @Override
            CheckovExecutionResult run(Path filePath, CheckovFramework framework) {
                return new CheckovExecutionResult(null, "unexpected runtime failure", -1);
            }
        };

        CheckovValidator validator = new CheckovValidator(runner, null);
        var doc = new io.polychro.spi.Document(null, yamlFile.toString());
        List<Diagnostic> diagnostics = validator.validate(doc);

        assertEquals(1, diagnostics.size());
        assertEquals(Severity.ERROR, diagnostics.get(0).severity());
        assertEquals("checkov-execution-error", diagnostics.get(0).code());
        assertEquals("unexpected runtime failure", diagnostics.get(0).message());
    }

    @Test
    void validateShouldReturnDiagnosticsOnSuccessfulRun() throws IOException {
        Path yamlFile = tempDir.resolve("test.yaml");
        Files.writeString(yamlFile, "name: test\n");

        String checkovOutput = """
                {
                  "results": {
                    "passed_checks": [],
                    "failed_checks": [
                      {
                        "check_id": "CKV_001",
                        "check_name": "Security issue found",
                        "severity": "HIGH",
                        "file_path": "/test.yaml",
                        "file_line_range": [1, 1]
                      }
                    ]
                  }
                }
                """;

        CheckovRunner runner = new CheckovRunner("checkov", 60, List.of(), null) {
            @Override
            CheckovExecutionResult run(Path filePath, CheckovFramework framework) {
                return new CheckovExecutionResult(checkovOutput, null, 1);
            }
        };

        CheckovValidator validator = new CheckovValidator(runner, null);
        var doc = new io.polychro.spi.Document(null, yamlFile.toString());
        List<Diagnostic> diagnostics = validator.validate(doc);

        assertEquals(1, diagnostics.size());
        assertEquals(Severity.ERROR, diagnostics.get(0).severity());
        assertEquals("CKV_001", diagnostics.get(0).code());
    }

    @Test
    void validateShouldDetectNotInstalledFromCannotFind() throws IOException {
        Path yamlFile = tempDir.resolve("test.yaml");
        Files.writeString(yamlFile, "name: test\n");

        CheckovRunner runner = new CheckovRunner("checkov", 60, List.of(), null) {
            @Override
            CheckovExecutionResult run(Path filePath, CheckovFramework framework) {
                return new CheckovExecutionResult(null, "cannot find the specified file", -1);
            }
        };

        CheckovValidator validator = new CheckovValidator(runner, null);
        var doc = new io.polychro.spi.Document(null, yamlFile.toString());
        List<Diagnostic> diagnostics = validator.validate(doc);

        assertEquals(1, diagnostics.size());
        assertEquals("checkov-not-installed", diagnostics.get(0).code());
    }

    @Test
    void validateShouldDetectNotInstalledFromNoSuchFile() throws IOException {
        Path yamlFile = tempDir.resolve("test.yaml");
        Files.writeString(yamlFile, "name: test\n");

        CheckovRunner runner = new CheckovRunner("checkov", 60, List.of(), null) {
            @Override
            CheckovExecutionResult run(Path filePath, CheckovFramework framework) {
                return new CheckovExecutionResult(null, "No such file or directory", -1);
            }
        };

        CheckovValidator validator = new CheckovValidator(runner, null);
        var doc = new io.polychro.spi.Document(null, yamlFile.toString());
        List<Diagnostic> diagnostics = validator.validate(doc);

        assertEquals(1, diagnostics.size());
        assertEquals("checkov-not-installed", diagnostics.get(0).code());
    }

    @Test
    void validateShouldDetectNotInstalledFromCreateProcess() throws IOException {
        Path yamlFile = tempDir.resolve("test.yaml");
        Files.writeString(yamlFile, "name: test\n");

        CheckovRunner runner = new CheckovRunner("checkov", 60, List.of(), null) {
            @Override
            CheckovExecutionResult run(Path filePath, CheckovFramework framework) {
                return new CheckovExecutionResult(null, "CreateProcess error=2", -1);
            }
        };

        CheckovValidator validator = new CheckovValidator(runner, null);
        var doc = new io.polychro.spi.Document(null, yamlFile.toString());
        List<Diagnostic> diagnostics = validator.validate(doc);

        assertEquals(1, diagnostics.size());
        assertEquals("checkov-not-installed", diagnostics.get(0).code());
    }

    @Test
    void validateShouldReturnEmptyListWhenCheckovPassesAll() throws IOException {
        Path yamlFile = tempDir.resolve("test.yaml");
        Files.writeString(yamlFile, "name: test\n");

        String checkovOutput = """
                {
                  "results": {
                    "passed_checks": [
                      {
                        "check_id": "CKV_001",
                        "check_name": "All good",
                        "severity": "HIGH",
                        "file_path": "/test.yaml",
                        "file_line_range": [1, 1]
                      }
                    ],
                    "failed_checks": []
                  }
                }
                """;

        CheckovRunner runner = new CheckovRunner("checkov", 60, List.of(), null) {
            @Override
            CheckovExecutionResult run(Path filePath, CheckovFramework framework) {
                return new CheckovExecutionResult(checkovOutput, null, 0);
            }
        };

        CheckovValidator validator = new CheckovValidator(runner, null);
        var doc = new io.polychro.spi.Document(null, yamlFile.toString());
        List<Diagnostic> diagnostics = validator.validate(doc);

        assertTrue(diagnostics.isEmpty());
    }
}
