package io.polychro.checkov;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CheckovCustomCheckTest {

    @TempDir
    Path tempDir;

    @Test
    void buildCommandShouldIncludeExternalChecksDir() {
        CheckovRunner runner = new CheckovRunner("checkov", 60, List.of(),
                "/path/to/custom-checks");

        List<String> command = runner.buildCommand(
                tempDir.resolve("file.yaml"), CheckovFramework.YAML);

        assertTrue(command.contains("--external-checks-dir"));
        int idx = command.indexOf("--external-checks-dir");
        assertEquals("/path/to/custom-checks", command.get(idx + 1));
    }

    @Test
    void buildCommandShouldNotIncludeExternalChecksDirWhenNull() {
        CheckovRunner runner = new CheckovRunner("checkov", 60, List.of(), null);

        List<String> command = runner.buildCommand(
                tempDir.resolve("file.yaml"), CheckovFramework.YAML);

        assertFalse(command.contains("--external-checks-dir"));
    }

    @Test
    void buildCommandShouldNotIncludeExternalChecksDirWhenBlank() {
        CheckovRunner runner = new CheckovRunner("checkov", 60, List.of(), "  ");

        List<String> command = runner.buildCommand(
                tempDir.resolve("file.yaml"), CheckovFramework.YAML);

        assertFalse(command.contains("--external-checks-dir"));
    }

    @Test
    void buildCommandShouldCombineSkipChecksAndCustomCheckDir() {
        CheckovRunner runner = new CheckovRunner("checkov", 60,
                List.of("CKV_001"), "/custom/policies");

        List<String> command = runner.buildCommand(
                tempDir.resolve("file.yaml"), CheckovFramework.YAML);

        assertTrue(command.contains("--skip-check"));
        assertTrue(command.contains("--external-checks-dir"));
        int skipIdx = command.indexOf("--skip-check");
        assertEquals("CKV_001", command.get(skipIdx + 1));
        int extIdx = command.indexOf("--external-checks-dir");
        assertEquals("/custom/policies", command.get(extIdx + 1));
    }
}
