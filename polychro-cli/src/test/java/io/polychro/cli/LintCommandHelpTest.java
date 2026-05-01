package io.polychro.cli;

import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.*;

class LintCommandHelpTest {

    @Test
    void helpShouldContainFormatFlag() {
        String help = getHelpOutput();
        assertTrue(help.contains("--format"));
    }

    @Test
    void helpShouldContainValidatorsFlag() {
        String help = getHelpOutput();
        assertTrue(help.contains("--validators"));
    }

    @Test
    void helpShouldContainRulesetFlag() {
        String help = getHelpOutput();
        assertTrue(help.contains("--ruleset"));
    }

    @Test
    void helpShouldContainSchemaFlag() {
        String help = getHelpOutput();
        assertTrue(help.contains("--schema"));
    }

    @Test
    void helpShouldContainConfigFlag() {
        String help = getHelpOutput();
        assertTrue(help.contains("--config"));
    }

    @Test
    void helpShouldContainFileParameter() {
        String help = getHelpOutput();
        assertTrue(help.contains("FILE"));
    }

    @Test
    void helpShouldContainDescription() {
        String help = getHelpOutput();
        assertTrue(help.contains("Lint one or more YAML/JSON files"));
    }

    @Test
    void versionShouldContainPolychro() {
        StringWriter out = new StringWriter();
        CommandLine cmd = new CommandLine(new PolychroCli());
        cmd.setOut(new PrintWriter(out));
        cmd.execute("--version");
        assertTrue(out.toString().contains("polychro"));
    }

    @Test
    void topLevelHelpShouldContainLintSubcommand() {
        StringWriter out = new StringWriter();
        CommandLine cmd = new CommandLine(new PolychroCli());
        cmd.setOut(new PrintWriter(out));
        cmd.execute("--help");
        assertTrue(out.toString().contains("lint"));
    }

    private String getHelpOutput() {
        StringWriter out = new StringWriter();
        CommandLine cmd = new CommandLine(new PolychroCli());
        cmd.setOut(new PrintWriter(out));
        cmd.execute("lint", "--help");
        return out.toString();
    }
}
