package io.polychro.markdown;

import com.fasterxml.jackson.databind.node.TextNode;
import io.polychro.spi.Diagnostic;
import io.polychro.spi.Document;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillFormatTest {

    private MarkdownValidator validator() {
        return new MarkdownValidator(120, "-", new SkillFormat(), new FrontmatterParser());
    }

    private Document doc(String content) {
        return new Document(new TextNode(content), "SKILL.md");
    }

    @Test
    void validateShouldAcceptValidSkillFile() {
        String content = "---\nname: my-skill\ndescription: A test skill\nversion: \"1.0\"\n---\n\n## Overview\n\nSome content.\n";
        List<Diagnostic> result = validator().validate(doc(content));
        assertTrue(result.stream().noneMatch(d -> d.code().startsWith("skill-")));
    }

    @Test
    void validateShouldReportMissingFrontmatter() {
        String content = "# My Skill\n\n## Overview\n\nContent.\n";
        List<Diagnostic> result = validator().validate(doc(content));
        assertTrue(result.stream().anyMatch(d -> d.code().equals("skill-missing-frontmatter")));
    }

    @Test
    void validateShouldReportMissingRequiredField() {
        String content = "---\ndescription: A test skill\n---\n\n## Overview\n\nContent.\n";
        List<Diagnostic> result = validator().validate(doc(content));
        assertTrue(result.stream().anyMatch(d -> d.code().equals("skill-missing-field")
                && d.message().contains("name")));
    }

    @Test
    void validateShouldReportEmptyDescription() {
        String content = "---\nname: my-skill\ndescription: \"\"\n---\n\n## Overview\n\nContent.\n";
        List<Diagnostic> result = validator().validate(doc(content));
        assertTrue(result.stream().anyMatch(d -> d.code().equals("skill-empty-field")));
    }

    @Test
    void validateShouldWarnWhenVersionNotQuoted() {
        String content = "---\nname: my-skill\ndescription: A test skill\nversion: 1.0\n---\n\n## Overview\n\nContent.\n";
        List<Diagnostic> result = validator().validate(doc(content));
        assertTrue(result.stream().anyMatch(d -> d.code().equals("skill-version-not-string")));
    }

    @Test
    void validateShouldWarnWhenNoSections() {
        String content = "---\nname: my-skill\ndescription: A test skill\n---\n\nJust some text without sections.\n";
        List<Diagnostic> result = validator().validate(doc(content));
        assertTrue(result.stream().anyMatch(d -> d.code().equals("skill-no-sections")));
    }

    @Test
    void validateShouldAcceptSkillWithoutVersion() {
        // version is optional — absence should not trigger version warning
        String content = "---\nname: my-skill\ndescription: A test skill\n---\n\n## Overview\n\nContent.\n";
        List<Diagnostic> result = validator().validate(doc(content));
        assertTrue(result.stream().noneMatch(d -> d.code().equals("skill-version-not-string")));
    }

    @Test
    void validateShouldAcceptNonTextualNameWithoutEmptyFieldError() {
        // name is a number — not null, not textual, so "empty" check doesn't apply
        String content = "---\nname: 123\ndescription: A test skill\n---\n\n## Overview\n\nContent.\n";
        List<Diagnostic> result = validator().validate(doc(content));
        assertTrue(result.stream().noneMatch(d -> d.code().equals("skill-empty-field")));
        assertTrue(result.stream().noneMatch(d -> d.code().equals("skill-missing-field")));
    }

    @Test
    void validateShouldAcceptBodyStartingWithSection() {
        // Body starts with ## directly (not after \n##)
        String content = "---\nname: my-skill\ndescription: A test skill\n---\n## Overview\n\nContent.\n";
        List<Diagnostic> result = validator().validate(doc(content));
        assertTrue(result.stream().noneMatch(d -> d.code().equals("skill-no-sections")));
    }

    @Test
    void validateShouldNotCrashWhenBodyIsNull() {
        // Test SkillFormat directly with null body
        SkillFormat format = new SkillFormat();
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.node.ObjectNode data = mapper.createObjectNode();
        data.put("name", "test");
        data.put("description", "desc");
        FrontmatterResult frontmatter = new FrontmatterResult(data, null, 1, null);
        java.util.List<io.polychro.spi.Diagnostic> diagnostics = new java.util.ArrayList<>();
        format.validate(new Document(null, "SKILL.md"), frontmatter, diagnostics);
        // Should not throw, and should not add skill-no-sections since body is null
        assertTrue(diagnostics.stream().noneMatch(d -> d.code().equals("skill-no-sections")));
    }
}
