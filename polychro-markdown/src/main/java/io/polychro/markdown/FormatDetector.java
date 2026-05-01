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
