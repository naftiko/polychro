package io.polychro.markdown;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class FormatAutoDetectionTest {

    @Test
    void detectShouldReturnSkillFormatForSkillMd() {
        MarkdownFormat format = FormatDetector.detect("path/to/SKILL.md");
        assertInstanceOf(SkillFormat.class, format);
    }

    @Test
    void detectShouldReturnAgentsFormatForAgentsMd() {
        MarkdownFormat format = FormatDetector.detect("path/to/AGENTS.md");
        assertInstanceOf(AgentsFormat.class, format);
    }

    @Test
    void detectShouldReturnInstructionsFormatForInstructionsMd() {
        MarkdownFormat format = FormatDetector.detect("path/to/foo.instructions.md");
        assertInstanceOf(InstructionsFormat.class, format);
    }

    @Test
    void detectShouldReturnInstructionsFormatForPromptMd() {
        MarkdownFormat format = FormatDetector.detect("path/to/bar.prompt.md");
        assertInstanceOf(InstructionsFormat.class, format);
    }

    @Test
    void detectShouldReturnGenericFormatForReadme() {
        MarkdownFormat format = FormatDetector.detect("path/to/README.md");
        assertInstanceOf(GenericFormat.class, format);
    }

    @Test
    void detectShouldReturnGenericFormatForNull() {
        MarkdownFormat format = FormatDetector.detect(null);
        assertInstanceOf(GenericFormat.class, format);
    }

    @Test
    void fromNameShouldReturnSkillFormat() {
        MarkdownFormat format = FormatDetector.fromName("skill");
        assertInstanceOf(SkillFormat.class, format);
    }

    @Test
    void fromNameShouldReturnAgentsFormat() {
        MarkdownFormat format = FormatDetector.fromName("agents");
        assertInstanceOf(AgentsFormat.class, format);
    }

    @Test
    void fromNameShouldReturnInstructionsFormat() {
        MarkdownFormat format = FormatDetector.fromName("instructions");
        assertInstanceOf(InstructionsFormat.class, format);
    }

    @Test
    void fromNameShouldReturnGenericFormatForUnknown() {
        MarkdownFormat format = FormatDetector.fromName("unknown");
        assertInstanceOf(GenericFormat.class, format);
    }

    @Test
    void fromNameShouldReturnGenericFormatForNull() {
        MarkdownFormat format = FormatDetector.fromName(null);
        assertInstanceOf(GenericFormat.class, format);
    }

    @Test
    void detectShouldHandleWindowsPath() {
        MarkdownFormat format = FormatDetector.detect("C:\\Users\\jlouv\\workspace\\SKILL.md");
        assertInstanceOf(SkillFormat.class, format);
    }

    @Test
    void detectShouldHandleFileNameOnly() {
        MarkdownFormat format = FormatDetector.detect("AGENTS.md");
        assertInstanceOf(AgentsFormat.class, format);
    }
}
