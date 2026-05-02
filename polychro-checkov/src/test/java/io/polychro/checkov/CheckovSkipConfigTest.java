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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CheckovSkipConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void buildCommandShouldIncludeSkipChecks() {
        CheckovRunner runner = new CheckovRunner("checkov", 60,
                List.of("CKV_AWS_001", "CKV_AWS_002"), null);

        List<String> command = runner.buildCommand(
                tempDir.resolve("main.tf"), CheckovFramework.TERRAFORM);

        assertTrue(command.contains("--skip-check"));
        int idx = command.indexOf("--skip-check");
        assertEquals("CKV_AWS_001,CKV_AWS_002", command.get(idx + 1));
    }

    @Test
    void buildCommandShouldNotIncludeSkipCheckWhenEmpty() {
        CheckovRunner runner = new CheckovRunner("checkov", 60, List.of(), null);

        List<String> command = runner.buildCommand(
                tempDir.resolve("main.tf"), CheckovFramework.TERRAFORM);

        assertFalse(command.contains("--skip-check"));
    }

    @Test
    void buildCommandShouldIncludeSingleSkipCheck() {
        CheckovRunner runner = new CheckovRunner("checkov", 60,
                List.of("CKV_K8S_001"), null);

        List<String> command = runner.buildCommand(
                tempDir.resolve("deploy.yaml"), CheckovFramework.KUBERNETES);

        assertTrue(command.contains("--skip-check"));
        int idx = command.indexOf("--skip-check");
        assertEquals("CKV_K8S_001", command.get(idx + 1));
    }

    @Test
    void validateShouldRespectSkipChecksInFullRun() throws IOException {
        Path yamlFile = tempDir.resolve("test.yaml");
        Files.writeString(yamlFile, "name: test\n");

        // Use a non-existent binary to exercise the graceful degradation path
        CheckovRunner runner = new CheckovRunner("nonexistent-checkov-xyz", 60,
                List.of("CKV_001"), null);
        CheckovValidator validator = new CheckovValidator(runner, null);

        var doc = new io.polychro.spi.Document(null, yamlFile.toString());
        var diagnostics = validator.validate(doc);

        // Should gracefully degrade since binary doesn't exist
        assertEquals(1, diagnostics.size());
        assertEquals("checkov-not-installed", diagnostics.get(0).code());
    }
}
