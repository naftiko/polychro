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

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
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

    private Document projectedDoc(com.fasterxml.jackson.databind.JsonNode frontmatter,
                                  com.fasterxml.jackson.databind.JsonNode... headings) {
        var root = JsonNodeFactory.instance.objectNode();
        var document = root.putObject("document");
        if (frontmatter == null) {
            document.putNull("frontmatter");
        } else {
            document.set("frontmatter", frontmatter);
        }
        var blocks = document.putArray("blocks");
        for (var heading : headings) {
            var block = JsonNodeFactory.instance.objectNode();
            block.setAll((com.fasterxml.jackson.databind.node.ObjectNode) heading.deepCopy());
            block.put("type", "heading");
            blocks.add(block);
        }
        return new Document(root, "markdown", "AGENTS.md");
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
        boolean found = format.containsSection(projectedDoc(null), "Build & Test");
        assertTrue(!found);
    }

    @Test
    void validateShouldHandleMissingProjectedHeadings() {
        AgentsFormat format = new AgentsFormat();
        List<Diagnostic> diagnostics = new java.util.ArrayList<>();
        format.validate(projectedDoc(null), diagnostics);
        assertTrue(diagnostics.stream().anyMatch(d -> d.code().equals("agents-missing-section")));
    }

    @Test
    void containsSectionShouldUseProjectedHeadings() {
        AgentsFormat format = new AgentsFormat();
        var heading = JsonNodeFactory.instance.objectNode();
        heading.put("level", 2);
        heading.put("text", "Build & Test");

        boolean found = format.containsSection(projectedDoc(null, heading), "Build & Test");

        assertTrue(found);
    }
}
