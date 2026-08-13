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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.polychro.conformance.golden.GoldenFileAssertions;
import io.polychro.conformance.model.NormalizedDiagnostic;
import io.polychro.conformance.runner.PolychroRunner;
import io.polychro.ruleset.Function;
import io.polychro.ruleset.Ruleset;
import io.polychro.rulesets.RulesetCatalog;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Polychro-only regression baseline for the bundled Ikanos rulesets shipped in
 * {@code polychro-rulesets} ({@code governance}, {@code ai-safety}, {@code security},
 * {@code mcp}, {@code consistency}, {@code resilience}, {@code agents}).
 *
 * <p>Unlike {@link ConformanceHarnessTest}, this class does <strong>not</strong> involve
 * Spectral: these rulesets are Ikanos-capability-centric ({@code $.capability.consumes},
 * {@code $.info}, ...), a domain Spectral has no concept of, so there is nothing on the
 * Spectral side to diff against. The golden file for each bundled ruleset is simply the stable
 * list of diagnostics Polychro reports today against a representative "violations" fixture.
 * That list is meaningful entirely on its own: if a future engine change (a JSONPath evaluation
 * change, a built-in function fix, a severity-mapping tweak, ...) silently alters what these
 * rulesets report, the corresponding dynamic test goes red — the same non-regression tripwire
 * {@link ConformanceHarnessTest} provides for Spectral-comparable rulesets, but for the engine's
 * own bundled content.
 *
 * <p>One {@link DynamicTest} is generated per {@link RulesetCatalog#available() available}
 * bundled ruleset name, each reading its fixture document from
 * {@code src/test/resources/baseline/<name>/<name>.<ext>} (see {@link #resolveDocumentPath})
 * and its golden diagnostics from {@code src/test/resources/baseline/<name>/golden-diagnostics.json}.
 * Adding a bundled ruleset to {@code polychro-rulesets} therefore only requires adding its
 * baseline fixture directory here — no new Java method.
 *
 * <p>Rulesets are loaded via {@link PolychroRunner#run(Path, Path)} (a real ruleset file on
 * disk) rather than an inline-content overload: {@code ai-safety.yml} declares
 * {@code extends: governance.yml} — a relative <em>file</em> reference resolved by
 * {@code RulesetComposer} against the ruleset's own parent directory — which only works when
 * every bundled ruleset is materialized as a sibling file. {@link #extractCatalogToTempDir}
 * copies all {@link RulesetCatalog#available() available} ruleset resources into one shared
 * directory once, so {@code extends} references between them resolve exactly as they do when
 * {@code polychro-rulesets} loads them from {@code src/main/resources/rulesets/} directly.
 *
 * <p>This class never skips: it has no Node/Spectral dependency, so it always runs, including
 * with {@code -Dpolychro.conformance.skipNodeBootstrap=true}.
 */
class BundledRulesetRegressionTest {

    private static final Path BASELINE = Path.of("src/test/resources/baseline").toAbsolutePath();

    @TempDir
    static Path rulesetsDir;

    /**
     * Candidate document file extensions, tried in order, for a bundled ruleset's fixture.
     */
    private static final List<String> DOCUMENT_FILE_EXTENSIONS = List.of("yaml", "md");

    /**
     * Materializes every bundled ruleset into one shared directory (see class Javadoc) once for
     * the whole test class, so {@code extends} references between bundled rulesets (e.g.
     * {@code ai-safety.yml} → {@code governance.yml}) resolve as sibling files.
     *
     * <p>{@code rulesetsDir} is a JUnit-managed {@code @TempDir}: it is injected before this
     * method runs and cleaned up automatically after the class, so no directory is leaked into
     * the system temp dir across test-class executions.
     */
    @BeforeAll
    static void extractCatalogToTempDir() throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        for (String name : RulesetCatalog.available()) {
            String rulesetContent = RulesetCatalog.load(name);
            Files.writeString(rulesetsDir.resolve(name + ".yml"), rulesetContent);

            JsonNode functionsDirectoryNode = mapper.readTree(rulesetContent).get("functionsDir");
            if (functionsDirectoryNode != null) {
                String functionsDirectory = functionsDirectoryNode.asText(".");
                Files.createDirectories(rulesetsDir.resolve(functionsDirectory));
                Ruleset ruleset = RulesetCatalog.loadAsRuleset(name);
                for (Function function : ruleset.functions()) {
                    Files.writeString(rulesetsDir.resolve(functionsDirectory).resolve(function.filename()),
                            function.sourceCode());
                }
            }
        }
    }

    /**
     * Discovers every bundled ruleset name and returns one {@link DynamicTest} per ruleset,
     * named after it (e.g. {@code governance}, {@code ai-safety}).
     */
    @TestFactory
    Stream<DynamicTest> bundledRulesetBaselines() {
        return RulesetCatalog.available().stream()
                .map(name -> DynamicTest.dynamicTest(name, () -> assertMatchesBaselineGolden(name)));
    }

    private void assertMatchesBaselineGolden(String rulesetName) {
        Path fixtureDir = BASELINE.resolve(rulesetName);
        Path documentPath = resolveDocumentPath(fixtureDir);
        Path goldenFile = fixtureDir.resolve("golden-diagnostics.json");
        Path rulesetPath = rulesetsDir.resolve(rulesetName + ".yml");

        // includeNonRecommended=true: exercise the ruleset's full content, not just its
        // recommended subset — required for "resilience" in particular, whose rules are all
        // deliberately marked recommended: false (see PolychroRunner#run javadoc); harmless for
        // the other bundled rulesets, none of which mark any rule non-recommended.
        List<NormalizedDiagnostic> diagnostics = PolychroRunner.run(documentPath, rulesetPath, true);

        GoldenFileAssertions.assertMatchesGolden(diagnostics, goldenFile);
    }

    /**
     * Resolves the fixture document for a bundled ruleset, trying each of
     * {@link #rulesetsDir} in order.
     */
    private static Path resolveDocumentPath(Path fixtureDir) {
        for (String fileExtension : DOCUMENT_FILE_EXTENSIONS) {
            Path candidate = fixtureDir.resolve(fixtureDir.getFileName() + "." + fileExtension);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }

        throw new IllegalStateException(
                "No fixture document with name %s and extension in (%s) found under %s".formatted(fixtureDir.getFileName(),
                        String.join(", ", DOCUMENT_FILE_EXTENSIONS), fixtureDir));
    }
}
