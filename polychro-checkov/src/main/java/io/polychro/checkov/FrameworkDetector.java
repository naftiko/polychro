package io.polychro.checkov;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Detects the Checkov framework from file extension and content.
 */
class FrameworkDetector {

    CheckovFramework detect(Path filePath) {
        String fileName = filePath.getFileName().toString().toLowerCase();

        if (fileName.endsWith(".tf") || fileName.endsWith(".tf.json")) {
            return CheckovFramework.TERRAFORM;
        }
        if (fileName.equals("dockerfile") || fileName.startsWith("dockerfile.")) {
            return CheckovFramework.DOCKERFILE;
        }

        if (fileName.endsWith(".yml") || fileName.endsWith(".yaml")) {
            return detectFromContent(filePath);
        }

        return CheckovFramework.YAML;
    }

    CheckovFramework detectFromContent(Path filePath) {
        try {
            String content = Files.readString(filePath);
            return detectFromContentString(content);
        } catch (IOException e) {
            return CheckovFramework.YAML;
        }
    }

    CheckovFramework detectFromContentString(String content) {
        if (content.contains("apiVersion:") || content.contains("apiVersion :")) {
            return CheckovFramework.KUBERNETES;
        }
        if (content.contains("AWSTemplateFormatVersion")) {
            return CheckovFramework.CLOUDFORMATION;
        }
        return CheckovFramework.YAML;
    }
}
