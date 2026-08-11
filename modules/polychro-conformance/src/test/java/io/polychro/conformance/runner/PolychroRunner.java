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

import io.polychro.conformance.model.NormalizedDiagnostic;
import io.polychro.core.Linter;
import io.polychro.core.LinterConfig;
import io.polychro.spi.Diagnostic;
import io.polychro.spi.Document;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Runs Polychro's {@code ruleset} validator, in-process, against a document and Spectral-format
 * ruleset, and normalizes the resulting {@link Diagnostic}s into {@link NormalizedDiagnostic}, so
 * the conformance harness (naftiko/polychro#81) can diff them against {@link SpectralRunner}
 * output on the same (ruleset, document) pair.
 *
 * <p>Calls the {@link Linter} Java API directly — the same one {@code polychro-cli}'s
 * {@code LintCommand} drives — rather than shelling out to the packaged CLI jar, so no build/
 * packaging step is required before the harness tests run, and the resulting {@link Diagnostic}
 * objects are available without a JSON round-trip.
 *
 * <p>Only the {@code ruleset} validator is exercised: {@code polychro-conformance} depends solely
 * on {@code polychro-ruleset}(-polyglot) for {@link io.polychro.spi.ValidatorFactory} discovery,
 * so no other built-in validator (well-formedness, JSON Schema, ...) is on the test classpath to
 * add diagnostics Spectral has no equivalent for.
 */
public class PolychroRunner {

    /** Matches a single bracketed array-index segment, e.g. {@code [0]}. */
    private static final Pattern ARRAY_INDEX_SEGMENT = Pattern.compile("\\[(\\d+)]");

    private PolychroRunner() {
    }

    /**
     * Lints a document with a single Spectral-format ruleset loaded from a file, scoping the
     * {@link Linter} to only the {@code ruleset} validator (mirroring Spectral, which likewise
     * only ever runs ruleset rules) and returns the normalized diagnostics, in the order Polychro
     * reported them. Non-recommended rules are excluded, matching Spectral's own
     * {@code recommended} default.
     *
     * @param documentPath the document (YAML/JSON) to lint
     * @param rulesetPath  the Spectral-format ruleset to lint it with
     */
    public static List<NormalizedDiagnostic> run(Path documentPath, Path rulesetPath) {
        return run(documentPath, rulesetPath, false);
    }

    /**
     * Lints a document with a single Spectral-format ruleset loaded from a file, scoping the
     * {@link Linter} to only the {@code ruleset} validator, and optionally including
     * non-recommended rules — needed for bundled Ikanos rulesets whose rules are deliberately
     * marked {@code recommended: false} (e.g. {@code resilience.yml}, which is INFO-only and
     * opt-in by design; see {@code polychro-rulesets}' own {@code ResilienceRulesetTest}, which
     * likewise passes {@code includeNonRecommended: true}).
     *
     * @param documentPath          the document (YAML/JSON) to lint
     * @param rulesetPath           the Spectral-format ruleset to lint it with
     * @param includeNonRecommended whether to also evaluate rules marked {@code recommended: false}
     */
    public static List<NormalizedDiagnostic> run(Path documentPath,
                                                 Path rulesetPath,
                                                 boolean includeNonRecommended) {
        Document document = loadDocument(documentPath);
        Linter linter = buildRulesetOnlyLinter(Map.of(
                "rulesetPath", rulesetPath.toAbsolutePath().toString(),
                "includeNonRecommended", includeNonRecommended));

        return lintAndNormalize(document, linter);
    }

    private static List<NormalizedDiagnostic> lintAndNormalize(Document document, Linter linter) {
        return linter.lint(document).stream()
                .map(PolychroRunner::toNormalized)
                .toList();
    }

    private static Document loadDocument(Path documentPath) {
        try {
            String content = Files.readString(documentPath);
            // format = null: let Document.fromString resolve it from the file extension (falling
            // back to content sniffing), exactly like polychro-cli's LintCommand does.
            return Document.fromString(content, null, documentPath.toAbsolutePath().toString());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read document: " + documentPath, e);
        }
    }

    private static Linter buildRulesetOnlyLinter(Map<String, Object> rulesetConfig) {
        LinterConfig config = new LinterConfig(
                List.of("ruleset"),
                Map.of("ruleset", rulesetConfig),
                false,
                "json-schema");
        return Linter.builder().config(config).build();
    }

    private static NormalizedDiagnostic toNormalized(Diagnostic diagnostic) {
        String ruleId = diagnostic.code();
        String path = toComparablePath(diagnostic.path());
        String severity = diagnostic.severity().name().toLowerCase(Locale.ROOT);
        return new NormalizedDiagnostic(ruleId, path, severity);
    }

    /**
     * Converts a Polychro diagnostic path (e.g. {@code $.consumes[0].baseUri}, dot notation with
     * bracketed array indices — see {@code JsonPathEvaluator.toDotNotation}) into the same dotted,
     * bracket-free segment form {@link SpectralRunner} produces from Spectral's {@code path} array
     * (e.g. {@code consumes.0.baseUri}), so the two sides are directly comparable.
     *
     * <p>Root-level diagnostics ({@code $} with no further segments) normalize to the empty
     * string, matching {@link SpectralRunner}'s handling of an empty {@code path} array.
     */
    static String toComparablePath(String polychroPath) {
        if (polychroPath == null || polychroPath.isBlank() || polychroPath.equals("$")) {
            return "";
        }
        String path = polychroPath.startsWith("$.")
                ? polychroPath.substring(2)
                : polychroPath.startsWith("$") ? polychroPath.substring(1) : polychroPath;
        String comparablePath = ARRAY_INDEX_SEGMENT.matcher(path).replaceAll(".$1");
        return polychroPath.startsWith("$[") ? comparablePath.substring(1) : comparablePath;
    }
}
