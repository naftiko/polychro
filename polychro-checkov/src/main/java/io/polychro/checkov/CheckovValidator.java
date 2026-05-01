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
            return List.of(new Diagnostic(Severity.INFO, "checkov-no-file",
                    "Checkov requires a file path; skipping in-memory document", null, null));
        }

        Path filePath = Path.of(doc.sourcePath());
        if (!Files.exists(filePath)) {
            return List.of(new Diagnostic(Severity.INFO, "checkov-file-not-found",
                    "File not found: " + doc.sourcePath(), null, null));
        }

        CheckovFramework framework = resolveFramework(filePath);

        CheckovRunner.CheckovExecutionResult result = runner.run(filePath, framework);

        if (!result.isSuccess()) {
            String msg = result.error();
            if (msg.contains("not available") || msg.contains("No such file")
                    || msg.contains("cannot find") || msg.contains("CreateProcess")) {
                return List.of(new Diagnostic(Severity.INFO, "checkov-not-installed",
                        "Checkov is not installed or not on PATH; skipping security scan", null, null));
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
