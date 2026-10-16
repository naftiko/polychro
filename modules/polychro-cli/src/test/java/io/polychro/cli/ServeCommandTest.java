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
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ServeCommandTest {

    /** MCP port declared in polychro-capability.yml. */
    private static final int MCP_PORT = 3001;

    @TempDir
    Path tempDir;

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void executeShouldKeepServingUntilShutdownIsRequested() throws Exception {
        ServeCommand cmd = new ServeCommand();
        wireSpec(cmd);

        CompletableFuture<Integer> serve = CompletableFuture.supplyAsync(cmd::execute);
        try {
            awaitPortOpen(serve);

            // Regression: execute() used to return right after start(), so PolychroCli.main
            // called System.exit and the server was gone about 2 seconds after startup.
            Thread.sleep(3_000);
            assertFalse(serve.isDone(), "serve returned while the server should still be running");
            assertTrue(isPortOpen(), "MCP port no longer accepts connections");
        } finally {
            cmd.requestShutdown();
        }

        assertEquals(0, serve.get(30, TimeUnit.SECONDS));
        assertFalse(isPortOpen(), "MCP port still open after shutdown");
    }

    @Test
    void requestShutdownShouldBeSafeBeforeTheServerStarts() {
        ServeCommand cmd = new ServeCommand();
        wireSpec(cmd);

        assertDoesNotThrow(cmd::requestShutdown);
    }

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

    private static void awaitPortOpen(CompletableFuture<Integer> serve) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        while (!isPortOpen()) {
            if (serve.isDone()) {
                fail("serve ended before the MCP port opened, exit code " + serve.get());
            }
            if (System.nanoTime() > deadline) {
                fail("MCP port " + MCP_PORT + " did not open within 30 seconds");
            }
            Thread.sleep(100);
        }
    }

    private static boolean isPortOpen() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", MCP_PORT), 500);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
