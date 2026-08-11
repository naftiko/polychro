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
package io.polychro.conformance.runner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.polychro.conformance.model.NormalizedDiagnostic;
import org.junit.jupiter.api.Assumptions;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Shells out to the pinned Spectral CLI and normalizes its JSON diagnostics into
 * {@link NormalizedDiagnostic}, so the conformance harness (naftiko/polychro#81) can diff
 * them against {@link PolychroRunner} output on the same (ruleset, document) pair.
 *
 * <p>The binary is resolved from this module's own {@code target/spectral-harness/} directory,
 * where the {@code frontend-maven-plugin} bootstrap (see {@code pom.xml}) installs a pinned,
 * module-local Node + {@code @stoplight/spectral-cli}.
 *
 * <p>When that bootstrap has not run (e.g. {@code -Dpolychro.conformance.skipNodeBootstrap=true}
 * for an offline local build), {@link #assumeAvailable()} makes calling tests skip gracefully
 * instead of failing — CI always performs the bootstrap, so it never skips there.
 */
public class SpectralRunner {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    private final Path spectralBinary;

    private SpectralRunner(Path spectralBinary) {
        this.spectralBinary = spectralBinary;
    }

    /**
     * Resolves the pinned Spectral binary relative to this module's own build output.
     */
    public static SpectralRunner resolve() {
        return new SpectralRunner(resolveBinary());
    }

    /**
     * @return {@code true} when the pinned Spectral CLI has been bootstrapped and is executable
     */
    public static boolean isAvailable() {
        Path binary = resolveBinary();
        return Files.isRegularFile(binary) && Files.isExecutable(binary);
    }

    /**
     * JUnit assumption gate: skips the calling test (instead of failing the build) when the
     * pinned Spectral CLI has not been bootstrapped locally.
     */
    public static void assumeAvailable() {
        Assumptions.assumeTrue(isAvailable(),
                "Spectral CLI not bootstrapped locally (target/spectral-harness/node_modules/.bin) "
                        + "— rerun without -Dpolychro.conformance.skipNodeBootstrap=true, or rely on CI.");
    }

    /**
     * Runs Spectral against a document with the given ruleset and returns the normalized
     * diagnostics, in the order Spectral reported them. Spectral's non-zero exit code for
     * lint findings is expected and deliberately ignored here — only a missing/unparsable
     * output is treated as a harness failure.
     *
     * @param documentPath the document (YAML/JSON) to lint
     * @param rulesetPath  the Spectral-format ruleset to lint it with
     */
    public List<NormalizedDiagnostic> run(Path documentPath, Path rulesetPath) {
        List<String> spectralArgs = List.of(
                "lint",
                documentPath.toAbsolutePath().toString(),
                "--ruleset", rulesetPath.toAbsolutePath().toString(),
                "--format", "json",
                "--fail-severity", "hint"
        );
        List<String> command = buildCommand(spectralArgs);

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.environment().merge("PATH", nodeDirectory().toString(),
                (existing, prefix) -> prefix + File.pathSeparator + existing);

        try (ExecutorService drainers = Executors.newFixedThreadPool(2)) {
            Process process = builder.start();

            Future<String> stdoutFuture = drainers.submit(() -> readFully(process.getInputStream()));
            Future<String> stderrFuture = drainers.submit(() -> readFully(process.getErrorStream()));
            boolean completed = process.waitFor(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                throw new IllegalStateException(
                        "Spectral CLI timed out after " + TIMEOUT + " running: "
                                + String.join(" ", command));
            }

            String stdout = stdoutFuture.get();
            String stderr = stderrFuture.get();
            return parseDiagnostics(stdout, stderr, command);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to run Spectral CLI: " + spectralBinary, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while running Spectral CLI", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Failed to read Spectral CLI output", e.getCause());
        }
    }

    private List<NormalizedDiagnostic> parseDiagnostics(String stdout, String stderr, List<String> command) {
        String trimmed = stdout.strip();
        if (trimmed.isEmpty()) {
            // In --format json, an empty result set is a literal `[]`; a truly empty stdout
            // means Spectral failed before it could report anything (bad ruleset path, crash).
            throw new IllegalStateException(
                    "Spectral CLI produced no output.\nstderr:\n" + stderr
                            + "\ncommand: " + String.join(" ", command));
        }

        try {
            JsonNode root = MAPPER.readTree(trimmed);
            if (!root.isArray()) {
                throw new IllegalStateException("""
                        Spectral CLI JSON output was not an array.
                        stdout: %s
                        stderr: %s
                        command: %s""".formatted(trimmed, stderr, command));
            }
            List<NormalizedDiagnostic> diagnostics = new ArrayList<>();
            for (JsonNode entry : root) {
                diagnostics.add(toNormalized(entry));
            }
            return diagnostics;
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Spectral CLI output was not valid JSON.\nstdout:\n" + trimmed + "\nstderr:\n" + stderr, e);
        }
    }

    private NormalizedDiagnostic toNormalized(JsonNode entry) {
        String ruleId = entry.path("code").asText(null);
        String path = toDottedPath(entry.path("path"));
        // Sentinel -1 (rather than defaulting to "warn"): a missing/non-numeric severity field
        // is itself a signal Spectral's output shape changed, and must surface as "unknown"
        // (see #toSeverityName) instead of silently masquerading as a plausible "warn" diagnostic.
        String severity = toSeverityName(entry.path("severity").asInt(-1));
        return new NormalizedDiagnostic(ruleId, path, severity);
    }

    /**
     * Spectral's JSON output represents {@code path} as an array of segments
     * (e.g. {@code ["paths", "/pets", "get"]}); normalize it to a dotted string so it is
     * directly comparable with {@link PolychroRunner}'s normalized {@code path}.
     */
    private String toDottedPath(JsonNode pathArray) {
        if (!pathArray.isArray() || pathArray.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (JsonNode segment : pathArray) {
            if (!sb.isEmpty()) {
                sb.append('.');
            }
            sb.append(segment.asText());
        }
        return sb.toString();
    }

    /**
     * Spectral encodes severity numerically (0=error, 1=warn, 2=info, 3=hint). Normalize to
     * the same lowercase names used on the Polychro side so diffing doesn't need to know
     * either engine's raw encoding.
     */
    private String toSeverityName(int spectralSeverity) {
        return switch (spectralSeverity) {
            case 0 -> "error";
            case 1 -> "warn";
            case 2 -> "info";
            case 3 -> "hint";
            default -> "unknown";
        };
    }

    private String readFully(InputStream in) throws IOException {
        return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }

    /**
     * Builds the OS-appropriate process command line for invoking the resolved
     * {@link #spectralBinary} with {@code spectralArgs}.
     *
     * <p>On Windows, npm installs a {@code .cmd} shim (a batch script), which
     * {@link ProcessBuilder} cannot execute directly — {@code CreateProcess} requires an
     * actual executable, not a script; attempting to run it directly fails with
     * {@code CreateProcess error=193}. Route it through {@code cmd.exe /c} instead, which is
     * how {@code .cmd} files are normally invoked from a shell.
     */
    private List<String> buildCommand(List<String> spectralArgs) {
        if (isWindows()) {
            List<String> command = new ArrayList<>();
            command.add("cmd.exe");
            command.add("/c");
            command.add(spectralBinary.toString());
            command.addAll(spectralArgs);
            return command;
        }
        List<String> command = new ArrayList<>();
        command.add(spectralBinary.toString());
        command.addAll(spectralArgs);
        return command;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static Path resolveBinary() {
        Path binDir = Path.of(System.getProperty("user.dir"), "target", "spectral-harness",
                "node_modules", ".bin");
        String binaryName = isWindows() ? "spectral.cmd" : "spectral";
        return binDir.resolve(binaryName);
    }

    private static Path nodeDirectory() {
        return Path.of(System.getProperty("user.dir"), "target", "spectral-harness", "node");
    }
}
