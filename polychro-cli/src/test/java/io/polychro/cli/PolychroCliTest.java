package io.polychro.cli;

import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

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

    @CommandLine.Command(name = "crash")
    static class CrashCommand implements Runnable {
        @Override
        public void run() {
            throw new RuntimeException("test crash");
        }
    }
}
