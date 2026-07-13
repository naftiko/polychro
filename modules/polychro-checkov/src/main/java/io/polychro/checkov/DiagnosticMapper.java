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
package io.polychro.checkov;

import io.polychro.spi.Diagnostic;
import io.polychro.spi.Severity;
import io.polychro.spi.SourceRange;

import java.util.List;

/**
 * Maps Checkov CheckResult objects to Polychro Diagnostic records.
 */
class DiagnosticMapper {

    List<Diagnostic> map(List<CheckResult> results) {
        return results.stream()
                .filter(r -> "FAILED".equals(r.result()))
                .map(this::toDiagnostic)
                .toList();
    }

    Diagnostic toDiagnostic(CheckResult result) {
        Severity severity = mapSeverity(result.severity());
        String message = buildMessage(result);
        SourceRange range = null;
        if (result.startLine() > 0) {
            range = new SourceRange(result.startLine(), 1, result.endLine(), 1);
        }
        return new Diagnostic(severity, result.checkId(), message, result.filePath(), range);
    }

    Severity mapSeverity(String checkovSeverity) {
        if (checkovSeverity == null) {
            return Severity.WARN;
        }
        return switch (checkovSeverity.toUpperCase()) {
            case "CRITICAL", "HIGH" -> Severity.ERROR;
            case "MEDIUM" -> Severity.WARN;
            case "LOW" -> Severity.INFO;
            default -> Severity.WARN;
        };
    }

    String buildMessage(CheckResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append(result.checkName());
        if (result.guidelineUrl() != null && !result.guidelineUrl().isBlank()) {
            sb.append(" (").append(result.guidelineUrl()).append(")");
        }
        return sb.toString();
    }
}
