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
package io.polychro.conformance.model;

import io.polychro.conformance.runner.PolychroRunner;

import java.util.Comparator;

/**
 * A single Spectral diagnostic, normalized to the shape comparable with
 * {@link PolychroRunner}'s output: {@code (ruleId, path, severity)}.
 *
 * @param ruleId   the rule name that produced the diagnostic (Spectral's {@code code})
 * @param path     the dotted JSON path the diagnostic points at (Spectral's {@code path}
 *                 array joined with {@code .}), empty string for document-root diagnostics
 * @param severity lowercase severity name: {@code error}, {@code warn}, {@code info}, or
 *                 {@code hint}
 */
public record NormalizedDiagnostic(String ruleId, String path, String severity) {

    /**
     * Deterministic ordering by rule id, then path, then severity (nulls first) — the same
     * ordering {@link ConformanceDiff} sorts its two sides by. Neither engine guarantees a
     * stable diagnostic order across separate JVM/process runs (e.g. rule-matching that iterates
     * a {@code Map} or evaluates multiple {@code given} JSONPath expressions per rule), so any
     * golden file comparing or persisting a {@code List<NormalizedDiagnostic>} — whether a
     * two-sided {@link ConformanceDiff} or a single-engine baseline list — must sort by this
     * order first; comparing/serializing raw iteration order will intermittently fail on
     * content-identical results (discovered on the bundled-ruleset regression baseline,
     * naftiko/polychro#81 bullet 4: {@code governance}'s two {@code baseUri} rules could come
     * back in either order across runs).
     */
    public static final Comparator<NormalizedDiagnostic> ORDER = Comparator
            .comparing(NormalizedDiagnostic::ruleId, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(NormalizedDiagnostic::path, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(NormalizedDiagnostic::severity, Comparator.nullsFirst(Comparator.naturalOrder()));
}
