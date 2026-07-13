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

class CheckovTimeoutTest {

    @TempDir
    Path tempDir;

    @Test
    void buildCommandShouldUseSpecifiedCheckovPath() {
        CheckovRunner runner = new CheckovRunner("/usr/local/bin/checkov", 120, List.of(), null);

        List<String> command = runner.buildCommand(
                tempDir.resolve("main.tf"), CheckovFramework.TERRAFORM);

        assertEquals("/usr/local/bin/checkov", command.get(0));
    }

    @Test
    void runShouldReturnErrorForNonExistentBinary() throws IOException {
        Path yamlFile = tempDir.resolve("test.yaml");
        Files.writeString(yamlFile, "name: test\n");

        CheckovRunner runner = new CheckovRunner("nonexistent-binary-xyz", 5, List.of(), null);
        CheckovRunner.CheckovExecutionResult result = runner.run(yamlFile, CheckovFramework.YAML);

        assertFalse(result.isSuccess());
        assertNotNull(result.error());
    }

    @Test
    void buildCommandShouldIncludeCorrectFramework() {
        CheckovRunner runner = new CheckovRunner("checkov", 30, List.of(), null);

        List<String> command = runner.buildCommand(
                tempDir.resolve("deploy.yaml"), CheckovFramework.KUBERNETES);

        int idx = command.indexOf("--framework");
        assertEquals("kubernetes", command.get(idx + 1));
    }

    @Test
    void buildCommandShouldIncludeJsonOutputAndCompact() {
        CheckovRunner runner = new CheckovRunner("checkov", 60, List.of(), null);

        List<String> command = runner.buildCommand(
                tempDir.resolve("file.yaml"), CheckovFramework.YAML);

        assertTrue(command.contains("--output"));
        int idx = command.indexOf("--output");
        assertEquals("json", command.get(idx + 1));
        assertTrue(command.contains("--compact"));
    }

    @Test
    void buildCommandShouldIncludeFileArgument() {
        CheckovRunner runner = new CheckovRunner("checkov", 60, List.of(), null);
        Path filePath = tempDir.resolve("config.yaml");

        List<String> command = runner.buildCommand(filePath, CheckovFramework.YAML);

        assertTrue(command.contains("--file"));
        int idx = command.indexOf("--file");
        assertEquals(filePath.toString(), command.get(idx + 1));
    }
}
