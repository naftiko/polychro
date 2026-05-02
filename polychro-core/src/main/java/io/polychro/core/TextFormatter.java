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
import io.polychro.spi.Severity;

import java.util.List;

/**
 * Formats diagnostics as human-readable text.
 */
public class TextFormatter implements DiagnosticFormatter {

    @Override
    public String format(List<Diagnostic> diagnostics) {
        if (diagnostics == null || diagnostics.isEmpty()) {
            return "No issues found.\n";
        }

        StringBuilder sb = new StringBuilder();
        for (Diagnostic d : diagnostics) {
            sb.append(formatSeverity(d.severity()));
            if (d.path() != null) {
                sb.append(" at ").append(d.path());
            }
            if (d.code() != null) {
                sb.append(" [").append(d.code()).append("]");
            }
            sb.append(": ").append(d.message());
            sb.append('\n');
        }
        sb.append('\n');
        sb.append(diagnostics.size()).append(" issue(s) found.\n");
        return sb.toString();
    }

    private String formatSeverity(Severity severity) {
        return switch (severity) {
            case ERROR -> "ERROR";
            case WARN -> "WARN";
            case INFO -> "INFO";
            case HINT -> "HINT";
        };
    }
}
