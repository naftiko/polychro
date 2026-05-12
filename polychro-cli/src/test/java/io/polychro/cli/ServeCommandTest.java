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
 */package io.polychro.cli;

import io.polychro.capability.PolychroCapability;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ServeCommandTest {

    @TempDir
    Path tempDir;

    @Test
    void executeShouldReturnTwoWhenConfigFileNotFound() {
        ServeCommand cmd = new ServeCommand();
        cmd.config = Path.of("nonexistent-config.yml");
        wireSpec(cmd);

        int exitCode = cmd.execute();
        assertEquals(2, exitCode);
    }

    @Test
    void buildCapabilityShouldCreateWithNoConfig() {
        ServeCommand cmd = new ServeCommand();
        wireSpec(cmd);

        PolychroCapability capability = cmd.buildCapability();
        assertNotNull(capability);
    }

    @Test
    void buildCapabilityShouldCreateWithConfig() throws Exception {
        Path configFile = tempDir.resolve(".polychro.yml");
        Files.writeString(configFile, "validators: []\nfailFast: false\n");

        ServeCommand cmd = new ServeCommand();
        cmd.config = configFile;
        wireSpec(cmd);

        PolychroCapability capability = cmd.buildCapability();
        assertNotNull(capability);
    }

    @Test
    void runShouldThrowExitException() {
        ServeCommand cmd = new ServeCommand();
        cmd.config = Path.of("nonexistent.yml");
        wireSpec(cmd);

        assertThrows(ExitException.class, cmd::run);
    }

    private void wireSpec(ServeCommand cmd) {
        CommandLine cmdLine = new CommandLine(cmd);
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        cmdLine.setOut(new PrintWriter(out));
        cmdLine.setErr(new PrintWriter(err));
        cmd.spec = cmdLine.getCommandSpec();
    }
}
