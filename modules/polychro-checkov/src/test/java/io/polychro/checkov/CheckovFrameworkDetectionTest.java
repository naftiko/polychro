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

import static org.junit.jupiter.api.Assertions.*;

class CheckovFrameworkDetectionTest {

    private final FrameworkDetector detector = new FrameworkDetector();

    @TempDir
    Path tempDir;

    @Test
    void detectShouldReturnTerraformForTfExtension() {
        Path tfFile = tempDir.resolve("main.tf");
        assertEquals(CheckovFramework.TERRAFORM, detector.detect(tfFile));
    }

    @Test
    void detectShouldReturnTerraformForTfJsonExtension() {
        Path tfJsonFile = tempDir.resolve("config.tf.json");
        assertEquals(CheckovFramework.TERRAFORM, detector.detect(tfJsonFile));
    }

    @Test
    void detectShouldReturnDockerfileForDockerfileName() {
        Path dockerfile = tempDir.resolve("Dockerfile");
        assertEquals(CheckovFramework.DOCKERFILE, detector.detect(dockerfile));
    }

    @Test
    void detectShouldReturnDockerfileForDockerfileDotVariant() {
        Path dockerfile = tempDir.resolve("Dockerfile.prod");
        assertEquals(CheckovFramework.DOCKERFILE, detector.detect(dockerfile));
    }

    @Test
    void detectShouldReturnKubernetesForYamlWithApiVersion() throws IOException {
        Path yamlFile = tempDir.resolve("deployment.yaml");
        Files.writeString(yamlFile, "apiVersion: apps/v1\nkind: Deployment\n");
        assertEquals(CheckovFramework.KUBERNETES, detector.detect(yamlFile));
    }

    @Test
    void detectShouldReturnCloudFormationForYamlWithAWSTemplate() throws IOException {
        Path yamlFile = tempDir.resolve("stack.yml");
        Files.writeString(yamlFile, "AWSTemplateFormatVersion: '2010-09-09'\nResources:\n");
        assertEquals(CheckovFramework.CLOUDFORMATION, detector.detect(yamlFile));
    }

    @Test
    void detectShouldReturnYamlForGenericYamlFile() throws IOException {
        Path yamlFile = tempDir.resolve("config.yaml");
        Files.writeString(yamlFile, "name: myconfig\nversion: 1\n");
        assertEquals(CheckovFramework.YAML, detector.detect(yamlFile));
    }

    @Test
    void detectShouldReturnYamlForUnreadableYamlFile() {
        Path yamlFile = tempDir.resolve("missing.yaml");
        assertEquals(CheckovFramework.YAML, detector.detect(yamlFile));
    }

    @Test
    void detectShouldReturnYamlForUnknownExtension() {
        Path otherFile = tempDir.resolve("config.toml");
        assertEquals(CheckovFramework.YAML, detector.detect(otherFile));
    }

    @Test
    void detectFromContentStringShouldReturnKubernetesForApiVersion() {
        assertEquals(CheckovFramework.KUBERNETES,
                detector.detectFromContentString("apiVersion: v1\nkind: Service"));
    }

    @Test
    void detectFromContentStringShouldReturnKubernetesForApiVersionWithSpace() {
        assertEquals(CheckovFramework.KUBERNETES,
                detector.detectFromContentString("apiVersion : apps/v1"));
    }

    @Test
    void detectFromContentStringShouldReturnCloudFormationForAWSTemplate() {
        assertEquals(CheckovFramework.CLOUDFORMATION,
                detector.detectFromContentString("AWSTemplateFormatVersion: '2010-09-09'"));
    }

    @Test
    void detectFromContentStringShouldReturnYamlForGenericContent() {
        assertEquals(CheckovFramework.YAML,
                detector.detectFromContentString("name: test\nport: 8080"));
    }
}
