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
