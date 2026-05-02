package io.polychro.cli;

import io.polychro.mcp.PolychroMcpServer;
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
    void buildServerShouldCreateServerWithNoConfig() {
        ServeCommand cmd = new ServeCommand();
        wireSpec(cmd);

        PolychroMcpServer server = cmd.buildServer();
        assertNotNull(server);
    }

    @Test
    void buildServerShouldCreateServerWithConfig() throws Exception {
        Path configFile = tempDir.resolve(".polychro.yml");
        Files.writeString(configFile, "validators: []\nfailFast: false\n");

        ServeCommand cmd = new ServeCommand();
        cmd.config = configFile;
        wireSpec(cmd);

        PolychroMcpServer server = cmd.buildServer();
        assertNotNull(server);
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
