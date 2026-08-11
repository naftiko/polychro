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
package io.polychro.conformance.golden;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.polychro.conformance.model.ConformanceDiff;
import io.polychro.conformance.model.NormalizedDiagnostic;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Loads/writes a {@link ConformanceDiff} as a committed golden file and asserts a freshly
 * computed diff matches it — the acceptance instrument for the conformance harness:
 * "Golden files capture the expected diff per fixture pair; an empty
 * diff means iso-functional for that pair."
 *
 * <p>A golden file is plain, human-reviewable JSON (the {@link ConformanceDiff} record shape),
 * so a PR diff on it visibly shows exactly which diagnostics started or stopped diverging —
 * e.g. when an engine fix closes a previously-documented gap, the golden file for that fixture
 * shrinks to {@code {"onlyInSpectral":[],"onlyInPolychro":[]}} and the harness test starts
 * failing until the file is updated — turning the fix into a visible, reviewable change instead
 * of a silent one.
 */
public class GoldenFileAssertions {

    /**
     * System property that, when {@code true}, makes {@link #assertMatchesGolden} write the
     * actual diff to the golden file instead of comparing against it — used to author or
     * deliberately update a fixture's golden file: {@code -Dpolychro.conformance.updateGoldenFiles=true}.
     */
    public static final String UPDATE_GOLDEN_FILES_PROPERTY = "polychro.conformance.updateGoldenFiles";

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private GoldenFileAssertions() {
    }

    /**
     * Asserts that {@code actual} matches the {@link ConformanceDiff} committed at
     * {@code goldenFile}.
     *
     * <p>When {@link #UPDATE_GOLDEN_FILES_PROPERTY} is set, this instead (over)writes
     * {@code goldenFile} with {@code actual} and returns without asserting — the file is then
     * expected to be reviewed and committed like any other test fixture change.
     *
     * @param actual     the diff freshly computed for the fixture under test
     * @param goldenFile the golden file path (typically under {@code src/test/resources/fixtures/})
     */
    public static void assertMatchesGolden(ConformanceDiff actual, Path goldenFile) {
        if (isUpdateMode()) {
            write(goldenFile, actual);
            return;
        }

        if (Files.notExists(goldenFile)) {
            Assertions.fail(missingGoldenFileMessage(goldenFile, actual));
            return;
        }

        ConformanceDiff expected = read(goldenFile);
        Assertions.assertEquals(expected, actual, mismatchMessage(goldenFile, expected, actual));
    }

    /**
     * Asserts that {@code actual} matches the diagnostic list committed at {@code goldenFile} —
     * the single-engine counterpart to {@link #assertMatchesGolden(ConformanceDiff, Path)}, used
     * by the bundled-Ikanos-ruleset regression baseline.
     * Those rulesets have no Spectral counterpart to diff against, so the golden file is simply the
     * stable list of diagnostics Polychro reports today; any future change to that list — an
     * added, removed, or altered diagnostic — is a regression signal on its own; no comparison
     * engine is needed to make it meaningful.
     *
     * <p>Same update-mode semantics as {@link #assertMatchesGolden(ConformanceDiff, Path)}:
     * {@link #UPDATE_GOLDEN_FILES_PROPERTY} (over)writes the file instead of asserting.
     *
     * @param actual     the diagnostics freshly computed for the bundled ruleset under test
     * @param goldenFile the golden file path (typically under {@code src/test/resources/baseline/})
     */
    public static void assertMatchesGolden(List<NormalizedDiagnostic> actual, Path goldenFile) {
        // Sort first: neither engine guarantees a stable diagnostic order across process runs
        // (see NormalizedDiagnostic#ORDER) — comparing/persisting raw iteration order would
        // intermittently fail on content-identical results.
        List<NormalizedDiagnostic> sortedActual = actual.stream()
                .sorted(NormalizedDiagnostic.ORDER)
                .toList();

        if (isUpdateMode()) {
            writeDiagnostics(goldenFile, sortedActual);
            return;
        }

        if (Files.notExists(goldenFile)) {
            Assertions.fail(missingGoldenFileMessage(goldenFile, sortedActual));
            return;
        }

        List<NormalizedDiagnostic> expected = readDiagnostics(goldenFile);
        Assertions.assertEquals(expected, sortedActual, mismatchMessage(goldenFile, expected, sortedActual));
    }

    /**
     * @return {@code true} when the harness is running in golden-file update mode
     *         (see {@link #UPDATE_GOLDEN_FILES_PROPERTY})
     */
    public static boolean isUpdateMode() {
        return Boolean.getBoolean(UPDATE_GOLDEN_FILES_PROPERTY);
    }

    /**
     * Reads and parses a committed golden file.
     *
     * @param goldenFile the golden file path
     * @return the parsed {@link ConformanceDiff}
     */
    public static ConformanceDiff read(Path goldenFile) {
        try {
            return MAPPER.readValue(goldenFile.toFile(), ConformanceDiff.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read golden file: " + goldenFile, e);
        }
    }

    /**
     * Reads and parses a committed golden file into a list of diagnostics.
     *
     * @param goldenFile the golden file path
     * @return the parsed list of {@link NormalizedDiagnostic}
     */
    public static List<NormalizedDiagnostic> readDiagnostics(Path goldenFile) {
        try {
            return MAPPER.readValue(goldenFile.toFile(), new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read golden file: " + goldenFile, e);
        }
    }

    /**
     * Serializes {@code diff} to {@code goldenFile} as pretty-printed JSON, creating parent
     * directories as needed.
     *
     * @param goldenFile the golden file path
     * @param diff       the diff to persist
     */
    public static void write(Path goldenFile, ConformanceDiff diff) {
        try {
            Files.createDirectories(goldenFile.toAbsolutePath().getParent());
            // Trailing newline so the committed file ends cleanly, matching typical formatter
            // conventions for checked-in JSON fixtures.
            String json = MAPPER.writeValueAsString(diff) + System.lineSeparator();
            Files.writeString(goldenFile, json);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write golden file: " + goldenFile, e);
        }
    }

    /**
     * Serializes {@code diagnostics} to {@code goldenFile} as a list of diagnostics.
     *
     * @param goldenFile the golden file path
     * @param diagnostics the diagnostics to persist
     */
    public static void writeDiagnostics(Path goldenFile, List<NormalizedDiagnostic> diagnostics) {
        try {
            Files.createDirectories(goldenFile.toAbsolutePath().getParent());
            // Trailing newline so the committed file ends cleanly, matching typical formatter
            // conventions for checked-in JSON fixtures.
            String json = MAPPER.writeValueAsString(diagnostics) + System.lineSeparator();
            Files.writeString(goldenFile, json);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write golden file: " + goldenFile, e);
        }
    }

    private static String missingGoldenFileMessage(Path goldenFile, ConformanceDiff actual) {
        return "Golden file not found: " + goldenFile.toAbsolutePath()
                + "\nRerun with -D" + UPDATE_GOLDEN_FILES_PROPERTY + "=true to create it from the "
                + "computed diff, then review and commit it."
                + "\nComputed diff was:\n" + toJson(actual);
    }

    private static String missingGoldenFileMessage(Path goldenFile, List<NormalizedDiagnostic> actual) {
        return "Golden file not found: " + goldenFile.toAbsolutePath()
                + "\nRerun with -D" + UPDATE_GOLDEN_FILES_PROPERTY + "=true to create it from the "
                + "computed diagnostics, then review and commit it."
                + "\nComputed diagnostics were:\n" + toJson(actual);
    }

    private static String mismatchMessage(Path goldenFile, ConformanceDiff expected, ConformanceDiff actual) {
        StringBuilder sb = new StringBuilder();
        sb.append("Conformance diff does not match golden file: ").append(goldenFile.toAbsolutePath())
                .append("\nIf this divergence is expected (an engine fix closed a gap, or introduced");
        sb.append(" one), rerun with -D").append(UPDATE_GOLDEN_FILES_PROPERTY)
                .append("=true to refresh it, then review and commit the change.\n");

        appendListDiff(sb, "onlyInSpectral", expected.onlyInSpectral(), actual.onlyInSpectral());
        appendListDiff(sb, "onlyInPolychro", expected.onlyInPolychro(), actual.onlyInPolychro());
        return sb.toString();
    }

    private static String mismatchMessage(Path goldenFile, List<NormalizedDiagnostic> expected,
            List<NormalizedDiagnostic> actual) {
        StringBuilder sb = new StringBuilder();
        sb.append("Diagnostics do not match golden file: ").append(goldenFile.toAbsolutePath())
                .append("\nIf this change is expected (a ruleset or engine change altered the ");
        sb.append("diagnostics), rerun with -D").append(UPDATE_GOLDEN_FILES_PROPERTY)
                .append("=true to refresh it, then review and commit the change.\n");

        appendListDiff(sb, "diagnostics", expected, actual);
        return sb.toString();
    }

    private static void appendListDiff(StringBuilder sb,
                                       String fieldName,
                                       List<NormalizedDiagnostic> expectedList,
                                       List<NormalizedDiagnostic> actualList) {
        List<NormalizedDiagnostic> newlyMissing = expectedList.stream()
                .filter(e -> !actualList.contains(e))
                .toList();
        List<NormalizedDiagnostic> newlyPresent = actualList.stream()
                .filter(a -> !expectedList.contains(a))
                .toList();

        if (newlyMissing.isEmpty() && newlyPresent.isEmpty()) {
            return;
        }
        sb.append('[').append(fieldName).append("] ");
        if (!newlyMissing.isEmpty()) {
            sb.append("no longer reported: ").append(newlyMissing).append("  ");
        }
        if (!newlyPresent.isEmpty()) {
            sb.append("newly reported: ").append(newlyPresent);
        }
        sb.append('\n');
    }

    private static String toJson(ConformanceDiff diff) {
        try {
            return MAPPER.writeValueAsString(diff);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String toJson(List<NormalizedDiagnostic> diagnostics) {
        try {
            return MAPPER.writeValueAsString(diagnostics);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
