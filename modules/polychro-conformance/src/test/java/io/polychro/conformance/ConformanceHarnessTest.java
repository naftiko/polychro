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
import io.polychro.conformance.runner.PolychroRunner;
import io.polychro.conformance.runner.SpectralRunner;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * The conformance harness itself: for each (ruleset, document) fixture
 * under {@code src/test/resources/fixtures/}, runs both Spectral and Polychro, normalizes and
 * diffs their diagnostics, and asserts the result against a committed golden file.
 *
 * <p>Fixtures are discovered automatically — every immediate subdirectory of {@code fixtures/}
 * that contains a {@code ruleset.yaml}, {@code document.yaml}, and {@code golden-diff.json}
 * becomes one {@link DynamicTest}. Adding a new fixture is therefore just adding those three
 * files under a new directory; no new Java method is needed.
 *
 * <p>An empty golden diff means Polychro is iso-functional with Spectral for that fixture — a
 * regression tripwire: if a future engine change silently breaks a currently-working case, the
 * corresponding dynamic test goes red. A non-empty golden diff documents a currently-known gap from
 * the naftiko/polychro analysis; when the gap closes, the corresponding test starts failing until
 * the golden file is refreshed with {@code -Dpolychro.conformance.updateGoldenFiles=true} —
 * turning the fix into a visible, reviewable change instead of a silent one.
 *
 * <p>All tests here require the pinned Spectral CLI (bootstrapped by this module's {@code pom.xml}
 * via {@code frontend-maven-plugin}); when unavailable locally (e.g.
 * {@code -Dpolychro.conformance.skipNodeBootstrap=true}), {@link #requireSpectral()} skips the
 * whole class rather than failing the build. CI always performs the bootstrap, so it never skips.
 */
class ConformanceHarnessTest {

    private static final Path FIXTURES = Path.of("src/test/resources/fixtures").toAbsolutePath();

    @BeforeAll
    static void requireSpectral() {
        SpectralRunner.assumeAvailable();
    }

    /**
     * Discovers every fixture directory and returns one {@link DynamicTest} per fixture, named
     * after its directory (e.g. {@code basic-truthy}, {@code function-options}).
     * Directories are sorted so test order — and therefore report order — is deterministic.
     */
    @TestFactory
    Stream<DynamicTest> conformanceFixtures() {
        return discoverFixtureDirs()
                .map(dir -> DynamicTest.dynamicTest(
                        dir.getFileName().toString(),
                        () -> assertMatchesFixtureGolden(dir)));
    }

    private Stream<Path> discoverFixtureDirs() {
        try (Stream<Path> entries = Files.list(FIXTURES)) {
            return entries
                    .filter(Files::isDirectory)
                    .filter(dir -> Files.isRegularFile(dir.resolve("ruleset.yaml"))
                            && Files.isRegularFile(dir.resolve("document.yaml"))
                            && Files.isRegularFile(dir.resolve("golden-diff.json")))
                    .sorted(Comparator.comparing(Path::getFileName))
                    .toList()
                    .stream();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to list fixtures directory: " + FIXTURES, e);
        }
    }

    private void assertMatchesFixtureGolden(Path fixtureDir) {
        Path rulesetPath = fixtureDir.resolve("ruleset.yaml");
        Path documentPath = fixtureDir.resolve("document.yaml");
        Path goldenFile = fixtureDir.resolve("golden-diff.json");

        List<NormalizedDiagnostic> spectralDiagnostics =
                SpectralRunner.resolve().run(documentPath, rulesetPath);
        List<NormalizedDiagnostic> polychroDiagnostics =
                PolychroRunner.run(documentPath, rulesetPath);

        ConformanceDiff diff = ConformanceDiff.diff(spectralDiagnostics, polychroDiagnostics);
        GoldenFileAssertions.assertMatchesGolden(diff, goldenFile);
    }
}
