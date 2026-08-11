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
package io.polychro.conformance;

import io.polychro.conformance.golden.GoldenFileAssertions;
import io.polychro.conformance.model.ConformanceDiff;
import io.polychro.conformance.model.NormalizedDiagnostic;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opentest4j.AssertionFailedError;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises {@link GoldenFileAssertions}' update-mode write path and error branches directly —
 * {@link io.polychro.conformance.ConformanceHarnessTest} and
 * {@link io.polychro.conformance.BundledRulesetRegressionTest} only ever run in the default,
 * successful comparison mode (comparing against an already-committed golden file), so the
 * distinct "golden file missing" and "update mode writes a fresh file" branches — which write
 * to committed source fixtures rather than just comparing against them — were previously
 * untested.
 *
 * <p>{@link #UPDATE_GOLDEN_FILES_PROPERTY} is a JVM-wide system property, so every test here
 * restores it in {@link #restoreUpdateModeProperty()} to avoid leaking update mode into any
 * other test class that shares the JVM (Surefire may fork one JVM per test class, but not
 * always — see {@code polychro-parent}'s Surefire configuration).
 */
class GoldenFileAssertionsTest {

    private static final String UPDATE_GOLDEN_FILES_PROPERTY = GoldenFileAssertions.UPDATE_GOLDEN_FILES_PROPERTY;

    @AfterEach
    void restoreUpdateModeProperty() {
        System.clearProperty(UPDATE_GOLDEN_FILES_PROPERTY);
    }

    @Test
    void assertMatchesGoldenShouldWriteConformanceDiffWhenUpdateModeEnabled(@TempDir Path tempDir) {
        Path goldenFile = tempDir.resolve("nested").resolve("golden-diff.json");
        ConformanceDiff diff = new ConformanceDiff(
                List.of(new NormalizedDiagnostic("only-spectral-rule", "info.description", "warn")),
                List.of(new NormalizedDiagnostic("only-polychro-rule", "info.tags", "info")));

        System.setProperty(UPDATE_GOLDEN_FILES_PROPERTY, "true");
        GoldenFileAssertions.assertMatchesGolden(diff, goldenFile);

        assertTrue(Files.exists(goldenFile), "Update mode must create the golden file, including parent directories");
        assertEquals(diff, GoldenFileAssertions.read(goldenFile));
    }

    @Test
    void assertMatchesGoldenShouldWriteDiagnosticListWhenUpdateModeEnabled(@TempDir Path tempDir) {
        Path goldenFile = tempDir.resolve("nested").resolve("golden-diagnostics.json");
        List<NormalizedDiagnostic> diagnostics = List.of(
                new NormalizedDiagnostic("consumer-timeout-declared", "capability.consumes.0.timeout", "info"));

        System.setProperty(UPDATE_GOLDEN_FILES_PROPERTY, "true");
        GoldenFileAssertions.assertMatchesGolden(diagnostics, goldenFile);

        assertTrue(Files.exists(goldenFile), "Update mode must create the golden file, including parent directories");
        assertEquals(diagnostics, GoldenFileAssertions.readDiagnostics(goldenFile));
    }

    @Test
    void assertMatchesGoldenShouldOverwriteExistingConformanceDiffWhenUpdateModeEnabled(@TempDir Path tempDir) {
        Path goldenFile = tempDir.resolve("golden-diff.json");
        GoldenFileAssertions.write(goldenFile, ConformanceDiff.diff(List.of(), List.of()));

        ConformanceDiff refreshed = new ConformanceDiff(
                List.of(new NormalizedDiagnostic("newly-diverging-rule", "info.description", "error")),
                List.of());

        System.setProperty(UPDATE_GOLDEN_FILES_PROPERTY, "true");
        GoldenFileAssertions.assertMatchesGolden(refreshed, goldenFile);

        assertEquals(refreshed, GoldenFileAssertions.read(goldenFile));
    }

    @Test
    void assertMatchesGoldenShouldFailWhenConformanceDiffGoldenFileMissing(@TempDir Path tempDir) {
        Path goldenFile = tempDir.resolve("does-not-exist.json");
        ConformanceDiff diff = ConformanceDiff.diff(List.of(), List.of());

        AssertionFailedError error = assertThrows(AssertionFailedError.class,
                () -> GoldenFileAssertions.assertMatchesGolden(diff, goldenFile));

        assertTrue(error.getMessage().contains(UPDATE_GOLDEN_FILES_PROPERTY),
                () -> "Expected the failure message to mention how to create the file, got: " + error.getMessage());
        assertFalse(Files.exists(goldenFile), "A missing-file failure must not create the golden file as a side effect");
    }

    @Test
    void assertMatchesGoldenShouldFailWhenDiagnosticListGoldenFileMissing(@TempDir Path tempDir) {
        Path goldenFile = tempDir.resolve("does-not-exist.json");
        List<NormalizedDiagnostic> diagnostics = List.of();

        AssertionFailedError error = assertThrows(AssertionFailedError.class,
                () -> GoldenFileAssertions.assertMatchesGolden(diagnostics, goldenFile));

        assertTrue(error.getMessage().contains(UPDATE_GOLDEN_FILES_PROPERTY),
                () -> "Expected the failure message to mention how to create the file, got: " + error.getMessage());
        assertFalse(Files.exists(goldenFile), "A missing-file failure must not create the golden file as a side effect");
    }

    @Test
    void assertMatchesGoldenShouldFailWithMismatchDetailsWhenConformanceDiffDiffers(@TempDir Path tempDir) {
        Path goldenFile = tempDir.resolve("golden-diff.json");
        ConformanceDiff committed = new ConformanceDiff(
                List.of(new NormalizedDiagnostic("known-gap-rule", "info.description", "error")),
                List.of());
        GoldenFileAssertions.write(goldenFile, committed);

        ConformanceDiff actual = ConformanceDiff.diff(List.of(), List.of());

        AssertionFailedError error = assertThrows(AssertionFailedError.class,
                () -> GoldenFileAssertions.assertMatchesGolden(actual, goldenFile));

        assertTrue(error.getMessage().contains(UPDATE_GOLDEN_FILES_PROPERTY),
                () -> "Expected the mismatch message to mention the update-mode refresh instructions, got: "
                        + error.getMessage());
    }

    @Test
    void assertMatchesGoldenShouldFailWithMismatchDetailsWhenDiagnosticListDiffers(@TempDir Path tempDir) {
        Path goldenFile = tempDir.resolve("golden-diagnostics.json");
        List<NormalizedDiagnostic> committed = List.of(
                new NormalizedDiagnostic("array-output-unbounded", "capability.exposes.0", "info"));
        GoldenFileAssertions.writeDiagnostics(goldenFile, committed);

        List<NormalizedDiagnostic> actual = List.of();

        AssertionFailedError error = assertThrows(AssertionFailedError.class,
                () -> GoldenFileAssertions.assertMatchesGolden(actual, goldenFile));

        assertTrue(error.getMessage().contains(UPDATE_GOLDEN_FILES_PROPERTY),
                () -> "Expected the mismatch message to mention the update-mode refresh instructions, got: "
                        + error.getMessage());
    }

    @Test
    void isUpdateModeShouldReflectSystemProperty() {
        System.clearProperty(UPDATE_GOLDEN_FILES_PROPERTY);
        assertFalse(GoldenFileAssertions.isUpdateMode());

        System.setProperty(UPDATE_GOLDEN_FILES_PROPERTY, "true");
        assertTrue(GoldenFileAssertions.isUpdateMode());
    }
}
