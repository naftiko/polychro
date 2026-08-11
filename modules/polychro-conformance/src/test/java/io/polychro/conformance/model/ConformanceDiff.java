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

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

/**
 * The normalized difference between Spectral's and Polychro's diagnostics for the same
 * (ruleset, document) pair — the comparable output of the conformance harness
 * (naftiko/polychro).
 *
 * <p>Both sides are diffed by full {@link NormalizedDiagnostic} equality (rule id, path, and
 * severity all together — the same diagnostic reported with a different severity counts as a
 * divergence on both sides, not a match). Diagnostics present in both engines are omitted
 * entirely: only the asymmetric remainder is kept, so an {@link #isEmpty() empty diff} is the
 * direct, easily-golden-filed signal of iso-functionality for that fixture.
 *
 * <p>Both lists are sorted (by rule id, then path, then severity — see
 * {@link NormalizedDiagnostic#ORDER}) so that two computations over the same underlying sets
 * always produce the same order, regardless of the order either engine reported its diagnostics
 * in — this is what makes the diff safe to serialize as a stable golden file.
 *
 * @param onlyInSpectral  diagnostics Spectral reported that Polychro did not
 * @param onlyInPolychro diagnostics Polychro reported that Spectral did not
 */
public record ConformanceDiff(
        List<NormalizedDiagnostic> onlyInSpectral,
        List<NormalizedDiagnostic> onlyInPolychro
) {

    public ConformanceDiff(List<NormalizedDiagnostic> onlyInSpectral, List<NormalizedDiagnostic> onlyInPolychro) {
        this.onlyInSpectral = List.copyOf(onlyInSpectral);
        this.onlyInPolychro = List.copyOf(onlyInPolychro);
    }

    /**
     * Computes the normalized diff between the two engines' diagnostics for the same
     * (ruleset, document) pair.
     *
     * <p>Diffs by multiset (bag) semantics, not set semantics: if one engine reports the same
     * normalized diagnostic twice and the other reports it only once, one occurrence remains
     * on the side that reported it more — a plain {@code List.contains}-based set difference
     * would remove every occurrence on both sides and silently declare that multiplicity
     * mismatch iso-functional.
     *
     * @param spectralDiagnostics  {@link io.polychro.conformance.runner.SpectralRunner} output
     * @param polychroDiagnostics {@link io.polychro.conformance.runner.PolychroRunner} output
     * @return the sorted, asymmetric remainder on each side; an empty diff when
     *         iso-functional
     */
    public static ConformanceDiff diff(List<NormalizedDiagnostic> spectralDiagnostics,
                                       List<NormalizedDiagnostic> polychroDiagnostics) {
        List<NormalizedDiagnostic> onlyInSpectral = removeMatchedOccurrences(spectralDiagnostics, polychroDiagnostics);
        List<NormalizedDiagnostic> onlyInPolychro = removeMatchedOccurrences(polychroDiagnostics, spectralDiagnostics);
        return new ConformanceDiff(onlyInSpectral, onlyInPolychro);
    }

    /**
     * Returns the elements of {@code from} that remain after removing one occurrence of each
     * element of {@code toRemove} — a multiset (bag) difference, unlike {@link List#contains},
     * which would remove every occurrence on both sides once a single match is found.
     */
    private static List<NormalizedDiagnostic> removeMatchedOccurrences(List<NormalizedDiagnostic> from,
                                                                       List<NormalizedDiagnostic> toRemove) {
        List<NormalizedDiagnostic> remaining = new ArrayList<>(toRemove);
        List<NormalizedDiagnostic> result = new ArrayList<>();
        for (NormalizedDiagnostic candidate : from) {
            if (!remaining.remove(candidate)) {
                result.add(candidate);
            }
        }
        return result.stream().sorted(NormalizedDiagnostic.ORDER).toList();
    }

    /**
     * @return {@code true} when both engines reported exactly the same diagnostics for the
     *         fixture — i.e. Polychro is iso-functional with Spectral on that (ruleset, document)
     *         pair
     */
    @JsonIgnore
    public boolean isEmpty() {
        return onlyInSpectral.isEmpty() && onlyInPolychro.isEmpty();
    }
}
