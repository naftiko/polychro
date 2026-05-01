package io.polychro.core;

import io.polychro.spi.Diagnostic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deduplicates diagnostics that have the same path and message.
 * When duplicates exist, the one with the highest severity (lowest ordinal) is kept.
 */
class DiagnosticDeduplicator {

    private DiagnosticDeduplicator() {
    }

    /**
     * Deduplicate a list of diagnostics.
     * Diagnostics with the same (path, message) pair are merged — the one with highest severity wins.
     *
     * @param diagnostics the input diagnostics (may contain duplicates)
     * @return deduplicated list
     */
    static List<Diagnostic> deduplicate(List<Diagnostic> diagnostics) {
        if (diagnostics == null || diagnostics.size() <= 1) {
            return diagnostics == null ? List.of() : diagnostics;
        }

        Map<String, Diagnostic> seen = new LinkedHashMap<>();

        for (Diagnostic d : diagnostics) {
            String key = deduplicationKey(d);
            Diagnostic existing = seen.get(key);
            if (existing == null) {
                seen.put(key, d);
            } else if (d.severity().ordinal() < existing.severity().ordinal()) {
                // Higher severity (lower ordinal) wins
                seen.put(key, d);
            }
        }

        return new ArrayList<>(seen.values());
    }

    private static String deduplicationKey(Diagnostic d) {
        String path = d.path() != null ? d.path() : "";
        String message = d.message() != null ? d.message() : "";
        return path + "\0" + message;
    }
}
