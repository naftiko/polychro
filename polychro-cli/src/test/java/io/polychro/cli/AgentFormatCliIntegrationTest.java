package io.polychro.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AgentFormatCliIntegrationTest {

    @TempDir
    Path tempDir;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void lintWithAgentFormatShouldProduceValidJson() throws Exception {
        Path file = createFile("test.yml", "name: test\n");
        ExecutionResult result = run("lint", "--format", "agent", file.toString());

        assertEquals(0, result.exitCode());
        JsonNode root = mapper.readTree(result.stdout());
        assertTrue(root.has("diagnostics"));
        assertTrue(root.has("summary"));
        assertTrue(root.has("tokens"));
    }

    @Test
    void agentFormatShouldContainNoAnsiCodes() throws Exception {
        Path file = createFile("test.yml", "name: test\n");
        ExecutionResult result = run("lint", "--format", "agent", file.toString());

        assertFalse(result.stdout().contains("\u001B["));
        assertFalse(result.stdout().contains("\033["));
    }

    @Test
    void agentFormatShouldHaveCorrectSummaryForCleanFile() throws Exception {
        Path file = createFile("test.yml", "name: test\nversion: \"1.0\"\n");
        ExecutionResult result = run("lint", "--format", "agent", file.toString());

        JsonNode root = mapper.readTree(result.stdout());
        assertEquals(0, root.get("summary").get("errors").asInt());
        assertEquals(0, root.get("summary").get("warnings").asInt());
        assertEquals(0, root.get("summary").get("info").asInt());
    }

    @Test
    void agentFormatShouldHavePositiveTokenCount() throws Exception {
        Path file = createFile("test.yml", "name: test\n");
        ExecutionResult result = run("lint", "--format", "agent", file.toString());

        JsonNode root = mapper.readTree(result.stdout());
        assertTrue(root.get("tokens").asInt() > 0);
    }

    @Test
    void agentFormatShouldProduceNoFramingText() throws Exception {
        Path file = createFile("test.yml", "name: test\n");
        ExecutionResult result = run("lint", "--format", "agent", file.toString());

        String output = result.stdout().trim();
        // Must start with { and end with } — pure JSON, no framing
        assertTrue(output.startsWith("{"));
        assertTrue(output.endsWith("}"));
    }

    private Path createFile(String name, String content) throws Exception {
        Path file = tempDir.resolve(name);
        Files.writeString(file, content);
        return file;
    }

    private ExecutionResult run(String... args) {
        StringWriter outWriter = new StringWriter();
        StringWriter errWriter = new StringWriter();
        PrintWriter out = new PrintWriter(outWriter);
        PrintWriter err = new PrintWriter(errWriter);

        CommandLine cmd = new CommandLine(new PolychroCli());
        cmd.setOut(out);
        cmd.setErr(err);
        cmd.setExecutionExceptionHandler((ex, commandLine, parseResult) -> {
            if (ex instanceof ExitException exitEx) {
                return exitEx.exitCode();
            }
            throw ex;
        });

        int exitCode = cmd.execute(args);
        return new ExecutionResult(exitCode, outWriter.toString(), errWriter.toString());
    }

    private record ExecutionResult(int exitCode, String stdout, String stderr) {}
}
