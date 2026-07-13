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
package io.polychro.markdown;

/**
 * Detects the Markdown format profile from the document's source path.
 */
final class FormatDetector {

    private FormatDetector() {
        // Utility class
    }

    static MarkdownFormat detect(String sourcePath) {
        if (sourcePath == null) {
            return new GenericFormat();
        }

        String fileName = extractFileName(sourcePath);

        if ("SKILL.md".equals(fileName)) {
            return new SkillFormat();
        }
        if ("AGENTS.md".equals(fileName)) {
            return new AgentsFormat();
        }
        if (fileName.endsWith(".instructions.md") || fileName.endsWith(".prompt.md")) {
            return new InstructionsFormat();
        }

        return new GenericFormat();
    }

    static MarkdownFormat fromName(String formatName) {
        if (formatName == null) {
            return new GenericFormat();
        }
        return switch (formatName) {
            case "skill" -> new SkillFormat();
            case "agents" -> new AgentsFormat();
            case "instructions" -> new InstructionsFormat();
            default -> new GenericFormat();
        };
    }

    private static String extractFileName(String path) {
        int lastSlash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        if (lastSlash >= 0) {
            return path.substring(lastSlash + 1);
        }
        return path;
    }
}
