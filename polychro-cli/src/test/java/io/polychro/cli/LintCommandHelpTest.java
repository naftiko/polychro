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
        assertTrue(help.contains("Lint one or more YAML/JSON/XML files"));
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
