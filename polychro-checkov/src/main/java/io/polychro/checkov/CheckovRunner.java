package io.polychro.checkov;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Invokes Checkov as a subprocess and captures its JSON output.
 */
class CheckovRunner {

    private final String checkovPath;
    private final long timeoutSeconds;
    private final List<String> skipChecks;
    private final String customCheckDir;

    CheckovRunner(String checkovPath, long timeoutSeconds, List<String> skipChecks, String customCheckDir) {
        this.checkovPath = checkovPath;
        this.timeoutSeconds = timeoutSeconds;
        this.skipChecks = skipChecks;
        this.customCheckDir = customCheckDir;
    }

    CheckovExecutionResult run(Path filePath, CheckovFramework framework) {
        List<String> command = buildCommand(filePath, framework);

        try {
            Process process = startProcess(command);

            boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

            if (!completed) {
                process.destroyForcibly();
                return new CheckovExecutionResult(null, "Checkov timed out after " + timeoutSeconds + "s", -1);
            }

            String output = readOutput(process);
            int exitCode = process.exitValue();
            return new CheckovExecutionResult(output, null, exitCode);
        } catch (IOException e) {
            return new CheckovExecutionResult(null, "Checkov not available: " + e.getMessage(), -1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new CheckovExecutionResult(null, "Checkov interrupted", -1);
        }
    }

    Process startProcess(List<String> command) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        return pb.start();
    }

    List<String> buildCommand(Path filePath, CheckovFramework framework) {
        List<String> command = new ArrayList<>();
        command.add(checkovPath);
        command.add("--file");
        command.add(filePath.toString());
        command.add("--framework");
        command.add(framework.value());
        command.add("--output");
        command.add("json");
        command.add("--compact");

        if (!skipChecks.isEmpty()) {
            command.add("--skip-check");
            command.add(String.join(",", skipChecks));
        }

        if (customCheckDir != null && !customCheckDir.isBlank()) {
            command.add("--external-checks-dir");
            command.add(customCheckDir);
        }

        return command;
    }

    private String readOutput(Process process) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }

    record CheckovExecutionResult(String output, String error, int exitCode) {
        boolean isSuccess() {
            return error == null;
        }
    }
}
