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

class CheckovValidatorTest {

    @TempDir
    Path tempDir;

    @Test
    void nameShouldReturnCheckov() {
        CheckovRunner runner = new CheckovRunner("checkov", 60, List.of(), null);
        CheckovValidator validator = new CheckovValidator(runner, null);
        assertEquals("checkov", validator.name());
    }

    @Test
    void validateShouldReturnInfoWhenSourcePathIsNull() {
        CheckovRunner runner = new CheckovRunner("checkov", 60, List.of(), null);
        CheckovValidator validator = new CheckovValidator(runner, null);

        var doc = new io.polychro.spi.Document(null, null);
        List<Diagnostic> diagnostics = validator.validate(doc);

        assertEquals(1, diagnostics.size());
        assertEquals(Severity.INFO, diagnostics.get(0).severity());
        assertEquals("checkov-no-file", diagnostics.get(0).code());
    }

    @Test
    void validateShouldReturnInfoWhenFileNotFound() {
        CheckovRunner runner = new CheckovRunner("checkov", 60, List.of(), null);
        CheckovValidator validator = new CheckovValidator(runner, null);

        var doc = new io.polychro.spi.Document(null, "/non/existent/file.yaml");
        List<Diagnostic> diagnostics = validator.validate(doc);

        assertEquals(1, diagnostics.size());
        assertEquals(Severity.INFO, diagnostics.get(0).severity());
        assertEquals("checkov-file-not-found", diagnostics.get(0).code());
    }

    @Test
    void validateShouldReturnInfoWhenCheckovNotInstalled() throws IOException {
        Path yamlFile = tempDir.resolve("test.yaml");
        Files.writeString(yamlFile, "name: test\n");

        CheckovRunner runner = new CheckovRunner("nonexistent-checkov-binary-xyz", 60, List.of(), null);
        CheckovValidator validator = new CheckovValidator(runner, null);

        var doc = new io.polychro.spi.Document(null, yamlFile.toString());
        List<Diagnostic> diagnostics = validator.validate(doc);

        assertEquals(1, diagnostics.size());
        assertEquals(Severity.INFO, diagnostics.get(0).severity());
        assertEquals("checkov-not-installed", diagnostics.get(0).code());
    }

    @Test
    void resolveFrameworkShouldReturnOverrideWhenValid() {
        CheckovRunner runner = new CheckovRunner("checkov", 60, List.of(), null);
        CheckovValidator validator = new CheckovValidator(runner, "terraform");

        CheckovFramework result = validator.resolveFramework(tempDir.resolve("file.yaml"));
        assertEquals(CheckovFramework.TERRAFORM, result);
    }

    @Test
    void resolveFrameworkShouldFallBackToDetectionWhenOverrideInvalid() throws IOException {
        Path tfFile = tempDir.resolve("main.tf");
        Files.writeString(tfFile, "resource \"aws_instance\" \"example\" {}");

        CheckovRunner runner = new CheckovRunner("checkov", 60, List.of(), null);
        CheckovValidator validator = new CheckovValidator(runner, "INVALID_FRAMEWORK");

        CheckovFramework result = validator.resolveFramework(tfFile);
        assertEquals(CheckovFramework.TERRAFORM, result);
    }

    @Test
    void resolveFrameworkShouldAutoDetectWhenNoOverride() throws IOException {
        Path k8sFile = tempDir.resolve("deployment.yaml");
        Files.writeString(k8sFile, "apiVersion: apps/v1\nkind: Deployment\n");

        CheckovRunner runner = new CheckovRunner("checkov", 60, List.of(), null);
        CheckovValidator validator = new CheckovValidator(runner, null);

        CheckovFramework result = validator.resolveFramework(k8sFile);
        assertEquals(CheckovFramework.KUBERNETES, result);
    }
}
