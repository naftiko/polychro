package io.polychro.markdown;

import com.fasterxml.jackson.databind.node.TextNode;
import io.polychro.spi.Diagnostic;
import io.polychro.spi.Document;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class InstructionsFormatTest {

    private MarkdownValidator validator() {
        return new MarkdownValidator(120, "-", new InstructionsFormat(), new FrontmatterParser());
    }

    private Document doc(String content) {
        return new Document(new TextNode(content), "coding.instructions.md");
    }

    @Test
    void validateShouldAcceptValidInstructionsFile() {
        String content = "---\napplyTo: \"**/*.java\"\ndescription: Java coding standards\n---\n\n# Instructions\n\nDo this.\n";
        List<Diagnostic> result = validator().validate(doc(content));
        assertTrue(result.stream().noneMatch(d -> d.code().startsWith("instructions-")));
    }

    @Test
    void validateShouldWarnOnMissingApplyTo() {
        String content = "---\ndescription: Some instructions\n---\n\n# Instructions\n\nDo this.\n";
        List<Diagnostic> result = validator().validate(doc(content));
        assertTrue(result.stream().anyMatch(d -> d.code().equals("instructions-missing-applyto")));
    }

    @Test
    void validateShouldReportEmptyApplyTo() {
        String content = "---\napplyTo: \"\"\n---\n\n# Instructions\n\nDo this.\n";
        List<Diagnostic> result = validator().validate(doc(content));
        assertTrue(result.stream().anyMatch(d -> d.code().equals("instructions-empty-applyto")));
    }

    @Test
    void validateShouldAcceptNoFrontmatterAsGenericFallback() {
        String content = "# Instructions\n\nJust plain instructions.\n";
        List<Diagnostic> result = validator().validate(doc(content));
        assertTrue(result.stream().noneMatch(d -> d.code().startsWith("instructions-")));
    }

    @Test
    void validateShouldAcceptNonTextualApplyTo() {
        // applyTo is a list — present but not textual, so empty-check doesn't trigger
        String content = "---\napplyTo:\n  - \"**/*.java\"\n  - \"**/*.kt\"\n---\n\n# Instructions\n\nDo this.\n";
        List<Diagnostic> result = validator().validate(doc(content));
        assertTrue(result.stream().noneMatch(d -> d.code().equals("instructions-empty-applyto")));
        assertTrue(result.stream().noneMatch(d -> d.code().equals("instructions-missing-applyto")));
    }
}
