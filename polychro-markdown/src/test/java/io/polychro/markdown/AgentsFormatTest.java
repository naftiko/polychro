package io.polychro.markdown;

import com.fasterxml.jackson.databind.node.TextNode;
import io.polychro.spi.Diagnostic;
import io.polychro.spi.Document;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentsFormatTest {

    private MarkdownValidator validator() {
        return new MarkdownValidator(120, "-", new AgentsFormat(), new FrontmatterParser());
    }

    private Document doc(String content) {
        return new Document(new TextNode(content), "AGENTS.md");
    }

    @Test
    void validateShouldAcceptValidAgentsFile() {
        String content = "# Agent Guidelines\n\n## Build & Test\n\nCommands.\n\n## Code Style\n\nStyle.\n\n## Contribution Workflow\n\nWorkflow.\n";
        List<Diagnostic> result = validator().validate(doc(content));
        assertTrue(result.stream().noneMatch(d -> d.code().startsWith("agents-")));
    }

    @Test
    void validateShouldWarnOnUnexpectedFrontmatter() {
        String content = "---\nname: agents\n---\n\n# Agent Guidelines\n\n## Build & Test\n\n## Code Style\n\n## Contribution Workflow\n";
        List<Diagnostic> result = validator().validate(doc(content));
        assertTrue(result.stream().anyMatch(d -> d.code().equals("agents-unexpected-frontmatter")));
    }

    @Test
    void validateShouldReportMissingBuildTestSection() {
        String content = "# Agent Guidelines\n\n## Code Style\n\nStyle.\n\n## Contribution Workflow\n\nWorkflow.\n";
        List<Diagnostic> result = validator().validate(doc(content));
        assertTrue(result.stream().anyMatch(d -> d.code().equals("agents-missing-section")
                && d.message().contains("Build & Test")));
    }

    @Test
    void validateShouldReportMissingCodeStyleSection() {
        String content = "# Agent Guidelines\n\n## Build & Test\n\nCommands.\n\n## Contribution Workflow\n\nWorkflow.\n";
        List<Diagnostic> result = validator().validate(doc(content));
        assertTrue(result.stream().anyMatch(d -> d.code().equals("agents-missing-section")
                && d.message().contains("Code Style")));
    }

    @Test
    void validateShouldReportMissingContributionWorkflowSection() {
        String content = "# Agent Guidelines\n\n## Build & Test\n\nCommands.\n\n## Code Style\n\nStyle.\n";
        List<Diagnostic> result = validator().validate(doc(content));
        assertTrue(result.stream().anyMatch(d -> d.code().equals("agents-missing-section")
                && d.message().contains("Contribution Workflow")));
    }

    @Test
    void validateShouldAcceptSectionAtH1Level() {
        // containsSection also checks "# " prefix
        String content = "# Build & Test\n\n# Code Style\n\n# Contribution Workflow\n";
        List<Diagnostic> result = validator().validate(doc(content));
        assertTrue(result.stream().noneMatch(d -> d.code().equals("agents-missing-section")));
    }

    @Test
    void containsSectionShouldReturnFalseForEmptyBody() {
        AgentsFormat format = new AgentsFormat();
        boolean found = format.containsSection("", "Build & Test");
        assertTrue(!found);
    }

    @Test
    void validateShouldHandleNullBody() {
        AgentsFormat format = new AgentsFormat();
        FrontmatterResult result = new FrontmatterResult(null, null, 1, null);
        List<Diagnostic> diagnostics = new java.util.ArrayList<>();
        Document nullDoc = new Document(null, "AGENTS.md");
        format.validate(nullDoc, result, diagnostics);
        // Should not throw, should report missing sections
        assertTrue(diagnostics.stream().anyMatch(d -> d.code().equals("agents-missing-section")));
    }
}
