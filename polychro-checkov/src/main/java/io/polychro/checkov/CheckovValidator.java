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
import io.polychro.spi.Document;
import io.polychro.spi.Severity;
import io.polychro.spi.Validator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Validates documents using Checkov security scanning.
 * <p>
 * Invokes Checkov as a subprocess, parses JSON output, and maps results to Diagnostics.
 * Gracefully degrades when Checkov is not installed.
 */
class CheckovValidator implements Validator {

    static final String NAME = "checkov";
    static final long DEFAULT_TIMEOUT_SECONDS = 60;

    private final CheckovRunner runner;
    private final CheckovOutputParser parser;
    private final DiagnosticMapper mapper;
    private final FrameworkDetector frameworkDetector;
    private final String frameworkOverride;

    CheckovValidator(CheckovRunner runner, String frameworkOverride) {
        this.runner = runner;
        this.parser = new CheckovOutputParser();
        this.mapper = new DiagnosticMapper();
        this.frameworkDetector = new FrameworkDetector();
        this.frameworkOverride = frameworkOverride;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public List<Diagnostic> validate(Document doc) {
        if (doc.sourcePath() == null) {
            // No physical file to scan — checkov requires a file path. Silently
            // skip rather than emit an INFO diagnostic; surfacing this on every
            // in-memory invocation pollutes output. See issue #20.
            return List.of();
        }

        Path filePath = Path.of(doc.sourcePath());
        if (!Files.exists(filePath)) {
            return List.of();
        }

        CheckovFramework framework = resolveFramework(filePath);

        CheckovRunner.CheckovExecutionResult result = runner.run(filePath, framework);

        if (!result.isSuccess()) {
            String msg = result.error();
            if (msg.contains("not available") || msg.contains("No such file")
                    || msg.contains("cannot find") || msg.contains("CreateProcess")) {
                // Checkov is not installed. Don't surface this on every lint
                // invocation — auto-discovered validators must stay silent when
                // they cannot run. See issue #20.
                return List.of();
            }
            return List.of(new Diagnostic(Severity.ERROR, "checkov-execution-error",
                    msg, null, null));
        }

        List<CheckResult> checkResults = parser.parse(result.output());
        return mapper.map(checkResults);
    }

    CheckovFramework resolveFramework(Path filePath) {
        if (frameworkOverride != null) {
            try {
                return CheckovFramework.valueOf(frameworkOverride.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Fall through to auto-detection
            }
        }
        return frameworkDetector.detect(filePath);
    }
}
