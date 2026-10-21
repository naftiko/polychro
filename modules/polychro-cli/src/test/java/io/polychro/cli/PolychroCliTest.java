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
import java.util.Properties;

import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.*;

class PolychroCliTest {

    @Test
    void executeShouldReturnZeroForHelp() {
        int exitCode = PolychroCli.execute(new String[]{"--help"});
        assertEquals(0, exitCode);
    }

    @Test
    void executeShouldReturnZeroForVersion() {
        int exitCode = PolychroCli.execute(new String[]{"--version"});
        assertEquals(0, exitCode);
    }

    @Test
    void executeShouldReturnZeroWithNoArgs() {
        int exitCode = PolychroCli.execute(new String[]{});
        assertEquals(0, exitCode);
    }

    @Test
    void buildCommandLineShouldHandleExitException() {
        StringWriter stdout = new StringWriter();
        StringWriter stderr = new StringWriter();
        CommandLine cmd = PolychroCli.buildCommandLine();
        cmd.setOut(new PrintWriter(stdout));
        cmd.setErr(new PrintWriter(stderr));
        // lint without files triggers picocli parameter error
        int exitCode = cmd.execute("lint");
        assertEquals(2, exitCode);
    }

    @Test
    void buildCommandLineShouldHandleNonExitException() {
        // Register a subcommand that throws a generic RuntimeException
        CommandLine cmd = PolychroCli.buildCommandLine();
        StringWriter stdout = new StringWriter();
        StringWriter stderr = new StringWriter();
        cmd.setOut(new PrintWriter(stdout));
        cmd.setErr(new PrintWriter(stderr));
        cmd.addSubcommand("crash", new CrashCommand());
        int exitCode = cmd.execute("crash");
        assertEquals(2, exitCode);
    }

    @Test
    void applyJvmDefaultsShouldSilenceTheInterpreterOnlyWarning() {
        Properties properties = new Properties();
        PolychroCli.applyJvmDefaults(properties);
        assertEquals("false", properties.getProperty("polyglot.engine.WarnInterpreterOnly"));
    }

    @Test
    void applyJvmDefaultsShouldKeepAValueSetByTheUser() {
        Properties properties = new Properties();
        properties.setProperty("polyglot.engine.WarnInterpreterOnly", "true");
        PolychroCli.applyJvmDefaults(properties);
        assertEquals("true", properties.getProperty("polyglot.engine.WarnInterpreterOnly"));
    }

    @CommandLine.Command(name = "crash")
    static class CrashCommand implements Runnable {
        @Override
        public void run() {
            throw new RuntimeException("test crash");
        }
    }
}
