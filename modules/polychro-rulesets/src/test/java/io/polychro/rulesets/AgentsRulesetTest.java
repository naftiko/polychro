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
package io.polychro.rulesets;

import io.polychro.markdown.MarkdownValidatorFactory;
import io.polychro.ruleset.Ruleset;
import io.polychro.ruleset.RulesetValidatorFactory;
import io.polychro.spi.Diagnostic;
import io.polychro.spi.Document;
import io.polychro.spi.Severity;
import io.polychro.spi.Validator;
import io.polychro.spi.ValidatorConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentsRulesetTest {

    private static Validator validator;
    private static final Path RULESETS = Path.of("src/main/resources/rulesets").toAbsolutePath();
    private static final Path FIXTURES = Path.of("src/test/resources/fixtures").toAbsolutePath();

    @BeforeAll
    static void setUp() {
        String rulesetPath = RULESETS.resolve("agents.yml").toString();
        validator = new RulesetValidatorFactory().create(
                new ValidatorConfig(Map.of("rulesetPath", rulesetPath)));
    }

    private static Document loadMarkdown(String fixtureName, String sourceFileName) throws Exception {
        String content = Files.readString(FIXTURES.resolve(fixtureName));
        return Document.fromString(content, "markdown", sourceFileName);
    }

    @Test
    void cleanAgentsMdShouldPassAgentsRuleset() throws Exception {
        Document doc = loadMarkdown("clean-agents.md", "AGENTS.md");
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.isEmpty(),
                () -> "Expected no agents violations but got: " + results);
    }

    @Test
    void agentsMdMissingSectionsShouldTriggerRequiredSections() throws Exception {
        Document doc = loadMarkdown("agents-violations.md", "AGENTS.md");
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.stream().anyMatch(d -> d.code().equals("agents-required-sections")),
                () -> "Expected agents-required-sections violation, got: " + results);
        assertTrue(results.stream()
                        .anyMatch(d -> d.message().contains("Code Style")),
                () -> "Expected a violation naming the missing 'Code Style' section, got: " + results);
        assertTrue(results.stream()
                        .anyMatch(d -> d.message().contains("Contribution Workflow")),
                () -> "Expected a violation naming the missing 'Contribution Workflow' section, got: "
                        + results);
        // "Build & Test" is present in the fixture — must not be flagged as missing.
        assertTrue(results.stream().noneMatch(d -> d.message().contains("Build & Test")),
                () -> "Did not expect 'Build & Test' to be reported missing, got: " + results);
    }

    @Test
    void cleanSkillMdShouldPassAgentsRuleset() throws Exception {
        Document doc = loadMarkdown("clean-skills.md", "SKILL.md");
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.isEmpty(),
                () -> "Expected no skill violations but got: " + results);
    }

    @Test
    void skillMdMissingDescriptionShouldTriggerRequiredFields() throws Exception {
        Document doc = loadMarkdown("skills-violations.md", "SKILL.md");
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.stream().anyMatch(d -> d.code().equals("skill-required-frontmatter")),
                () -> "Expected skill-required-frontmatter violation, got: " + results);
        assertTrue(results.stream().anyMatch(d -> d.message().contains("description")),
                () -> "Expected a violation naming the missing 'description' field, got: " + results);
        assertTrue(results.stream().anyMatch(d -> d.message().contains("name")),
                () -> "Expected a violation naming the invalid 'name' format, got: " + results);
        assertTrue(results.stream().allMatch(d -> d.severity() == Severity.ERROR),
                () -> "Expected skill-required-frontmatter diagnostics to be ERROR, got: " + results);
    }

    @Test
    void skillMdNameTooLongShouldTriggerRequiredFields() {
        // "a" repeated 65 times exceeds the spec's 64-character max for `name`.
        String longName = "a".repeat(65);
        String content = "---\nname: " + longName + "\ndescription: \"Valid description.\"\n---\n\n# Skill\n";
        Document doc = Document.fromString(content, "markdown", "SKILL.md");
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.stream().anyMatch(d -> d.code().equals("skill-required-frontmatter")
                        && d.message().contains("64")),
                () -> "Expected a violation about 'name' exceeding 64 characters, got: " + results);
    }

    @Test
    void skillMdDescriptionTooLongShouldTriggerRequiredFields() {
        // "a" repeated 1025 times exceeds the spec's 1024-character max for `description`.
        String longDescription = "a".repeat(1025);
        String content = "---\nname: valid-skill\ndescription: \"" + longDescription + "\"\n---\n\n# Skill\n";
        Document doc = Document.fromString(content, "markdown", "SKILL.md");
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.stream().anyMatch(d -> d.code().equals("skill-required-frontmatter")
                        && d.message().contains("1024")),
                () -> "Expected a violation about 'description' exceeding 1024 characters, got: " + results);
    }

    @Test
    void agentsRuleShouldNotFireOnSkillMd() throws Exception {
        // The AGENTS.md-only override must not leak onto a SKILL.md document, even
        // though clean-skills.md has no "Build & Test" / "Code Style" / "Contribution
        // Workflow" headings — proving the file-scoped override glob, not just the
        // required-sections options, is what gates the rule.
        Document doc = loadMarkdown("clean-skills.md", "SKILL.md");
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.stream().noneMatch(d -> d.code().equals("agents-required-sections")),
                () -> "agents-required-sections must not fire on a SKILL.md document, got: " + results);
    }

    @Test
    void skillRuleShouldNotFireOnAgentsMd() throws Exception {
        Document doc = loadMarkdown("clean-agents.md", "AGENTS.md");
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.stream().noneMatch(d -> d.code().equals("skill-required-frontmatter")),
                () -> "skill-required-frontmatter must not fire on an AGENTS.md document, got: " + results);
    }

    @Test
    void cleanInstructionsMdShouldPassAgentsRuleset() throws Exception {
        Document doc = loadMarkdown("clean-instructions.md", "coding.instructions.md");
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.isEmpty(),
                () -> "Expected no instructions violations but got: " + results);
    }

    @Test
    void instructionsMdWithNoFrontmatterShouldPassAgentsRuleset() throws Exception {
        Document doc = loadMarkdown("instructions-no-frontmatter.md", "coding.instructions.md");
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.isEmpty(),
                () -> "Expected no violations for a frontmatter-less .instructions.md, got: " + results);
    }

    @Test
    void instructionsMdMissingApplyToShouldTriggerApplyToPresence() throws Exception {
        Document doc = loadMarkdown("instructions-missing-applyto.md", "coding.instructions.md");
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.stream().anyMatch(d -> d.code().equals("instructions-applyto-presence")),
                () -> "Expected instructions-applyto-presence violation, got: " + results);
        assertTrue(results.stream()
                        .filter(d -> d.code().equals("instructions-applyto-presence"))
                        .allMatch(d -> d.severity() == Severity.WARN),
                () -> "Expected instructions-applyto-presence diagnostics to be WARN, got: " + results);
    }

    @Test
    void instructionsMdBlankApplyToShouldTriggerApplyToNotBlank() throws Exception {
        Document doc = loadMarkdown("instructions-blank-applyto.md", "coding.instructions.md");
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.stream().anyMatch(d -> d.code().equals("instructions-applyto-not-blank")),
                () -> "Expected instructions-applyto-not-blank violation, got: " + results);
        assertTrue(results.stream()
                        .filter(d -> d.code().equals("instructions-applyto-not-blank"))
                        .allMatch(d -> d.severity() == Severity.ERROR),
                () -> "Expected instructions-applyto-not-blank diagnostics to be ERROR, got: " + results);
        // A blank 'applyTo' is still present, so the presence check must not also fire.
        assertTrue(results.stream().noneMatch(d -> d.code().equals("instructions-applyto-presence")),
                () -> "Did not expect instructions-applyto-presence to fire when 'applyTo' is present, got: "
                        + results);
    }

    @Test
    void instructionsMdShouldAlsoDetectPromptMdExtension() throws Exception {
        Document doc = loadMarkdown("instructions-missing-applyto.md", "my-workflow.prompt.md");
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.stream().anyMatch(d -> d.code().equals("instructions-applyto-presence")),
                () -> "Expected instructions-applyto-presence violation on a .prompt.md file, got: " + results);
    }

    @Test
    void instructionsRuleShouldNotFireOnAgentsMd() throws Exception {
        Document doc = loadMarkdown("clean-agents.md", "AGENTS.md");
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.stream().noneMatch(d -> d.code().startsWith("instructions-")),
                () -> "instructions-* rules must not fire on an AGENTS.md document, got: " + results);
    }

    @Test
    void agentsRuleShouldNotFireOnInstructionsMd() throws Exception {
        Document doc = loadMarkdown("clean-instructions.md", "coding.instructions.md");
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.stream().noneMatch(d -> d.code().equals("agents-required-sections")),
                () -> "agents-required-sections must not fire on a .instructions.md document, got: " + results);
    }

    @Test
    void agentsRulesetContentShouldBeLoadableFromCatalog() {
        Ruleset ruleset = RulesetCatalog.loadAsRuleset("agents");
        assertNotNull(ruleset);
        assertTrue(ruleset.overrides().get(0).rules().containsKey("agents-required-sections"),
                "Expected agents.yml to declare the agents-required-sections rule");
        assertTrue(ruleset.overrides().get(1).rules().containsKey("skill-required-frontmatter"),
                "Expected agents.yml to declare the skill-required-frontmatter rule");
        assertTrue(ruleset.overrides().get(2).rules().containsKey("instructions-applyto-presence"),
                "Expected agents.yml to declare the instructions-applyto-presence rule");
    }

    @Test
    void rulesetValidatorAloneShouldNotDetectDeadLinksInAgentsMd() throws Exception {
        // dead-link-agents.md links to an inexistent.md that does not exist alongside it — but the
        // ruleset validator has no link-checking capability, so it must not report it.
        Document doc = loadMarkdown("dead-link-agents.md", "AGENTS.md");
        List<Diagnostic> results = validator.validate(doc);
        assertTrue(results.stream().noneMatch(d -> d.code().equals("broken-relative-link")),
                () -> "The ruleset validator has no link-checking logic and must never emit "
                        + "broken-relative-link, got: " + results);
    }

    @Test
    void markdownValidatorShouldDetectDeadLinkInDeadLinkAgentsFixture() throws Exception {
        Path fixturePath = FIXTURES.resolve("dead-link-agents.md");
        Document doc = Document.fromString(
                Files.readString(fixturePath), "markdown", fixturePath.toString());

        Validator markdownValidator = new MarkdownValidatorFactory()
                .createWithAutoDetect(new ValidatorConfig(Map.of()), fixturePath.toString());
        List<Diagnostic> results = markdownValidator.validate(doc);

        assertTrue(results.stream().anyMatch(d -> d.code().equals("broken-relative-link")),
                () -> "Expected the markdown validator to flag the dead ./inexistent.md link "
                        + "in dead-link-agents.md, got: " + results);
    }

    @Test
    void composedAgentsAndMarkdownValidatorsShouldTogetherCatchBothKindsOfViolations() throws Exception {
        String content = Files.readString(FIXTURES.resolve("agents-violations.md"));
        Document doc = Document.fromString(content, "markdown", "AGENTS.md");

        Validator markdownValidator = new MarkdownValidatorFactory()
                .createWithAutoDetect(new ValidatorConfig(Map.of()), "AGENTS.md");

        List<Diagnostic> combined = new ArrayList<>();
        combined.addAll(validator.validate(doc));
        combined.addAll(markdownValidator.validate(doc));

        assertTrue(combined.stream().anyMatch(d -> d.code().equals("agents-required-sections")),
                () -> "Expected the ruleset validator's agents-required-sections in the composed "
                        + "diagnostics, got: " + combined);
    }
}
