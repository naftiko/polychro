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
package io.polychro.cli;

import io.polychro.core.AgentFormatter;
import io.polychro.core.DiagnosticFormatter;
import io.polychro.core.JsonFormatter;
import io.polychro.core.Linter;
import io.polychro.core.LinterConfig;
import io.polychro.core.SarifFormatter;
import io.polychro.core.TextFormatter;
import io.polychro.spi.Diagnostic;
import io.polychro.spi.Document;
import io.polychro.spi.Severity;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The {@code lint} subcommand — validates one or more files.
 * <p>
 * Exit codes:
 * <ul>
 *   <li>0 — no issues found</li>
 *   <li>1 — warnings only</li>
 *   <li>2 — errors found</li>
 * </ul>
 */
@CommandLine.Command(
        name = "lint",
        mixinStandardHelpOptions = true,
        description = "Lint one or more YAML/JSON files"
)
public class LintCommand implements Runnable {

    @CommandLine.Parameters(
            paramLabel = "FILE",
            description = "Files to lint",
            arity = "1..*"
    )
    List<Path> files;

    @CommandLine.Option(
            names = {"--format", "-f"},
            description = "Output format: text, json, sarif, agent (default: text)",
            defaultValue = "text"
    )
    String format;

    @CommandLine.Option(
            names = {"--validators", "-v"},
            description = "Comma-separated list of validators to enable",
            split = ","
    )
    List<String> validators;

    @CommandLine.Option(
            names = {"--ruleset", "-r"},
            description = "Path to custom ruleset file"
    )
    Path ruleset;

    @CommandLine.Option(
            names = {"--schema", "-s"},
            description = "Path to custom schema file"
    )
    Path schema;

    @CommandLine.Option(
            names = {"--config", "-c"},
            description = "Path to .polychro.yml config file"
    )
    Path config;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        int exitCode = execute();
        throw new ExitException(exitCode);
    }

    int execute() {
        PrintWriter out = spec.commandLine().getOut();
        PrintWriter err = spec.commandLine().getErr();

        Linter linter = buildLinter(err);
        if (linter == null) {
            return 2;
        }

        List<Diagnostic> allDiagnostics = new ArrayList<>();

        for (Path file : files) {
            if (!Files.exists(file)) {
                err.println("Error: file not found: " + file);
                return 2;
            }
            Document doc = loadDocument(file, err);
            if (doc == null) {
                return 2;
            }
            allDiagnostics.addAll(linter.lint(doc));
        }

        DiagnosticFormatter formatter = resolveFormatter(format);
        out.print(formatter.format(allDiagnostics));
        out.flush();

        return computeExitCode(allDiagnostics);
    }

    Linter buildLinter(PrintWriter err) {
        Linter.Builder builder = Linter.builder();

        if (config != null) {
            if (!Files.exists(config)) {
                err.println("Error: config file not found: " + config);
                return null;
            }
            builder.config(config);
        } else {
            LinterConfig linterConfig = buildConfigFromFlags();
            builder.config(linterConfig);
        }

        return builder.build();
    }

    LinterConfig buildConfigFromFlags() {
        List<String> validatorList = validators != null ? validators : List.of();
        java.util.Map<String, java.util.Map<String, Object>> configMap = new java.util.LinkedHashMap<>();

        if (ruleset != null) {
            configMap.put("ruleset", java.util.Map.of("path", ruleset.toString()));
        }
        if (schema != null) {
            configMap.put("json-schema", java.util.Map.of("schema", schema.toString()));
        }

        return new LinterConfig(validatorList, configMap, false, "json-schema");
    }

    static DiagnosticFormatter resolveFormatter(String format) {
        return switch (format.toLowerCase()) {
            case "json" -> new JsonFormatter();
            case "sarif" -> new SarifFormatter();
            case "agent" -> new AgentFormatter();
            default -> new TextFormatter();
        };
    }

    static Document loadDocument(Path file, PrintWriter err) {
        String fileName = file.getFileName().toString().toLowerCase();
        try {
            if (fileName.endsWith(".json")) {
                return Document.fromJson(file);
            }
            return Document.fromYaml(file);
        } catch (Exception e) {
            err.println("Error: failed to parse " + file + ": " + e.getMessage());
            return null;
        }
    }

    static int computeExitCode(List<Diagnostic> diagnostics) {
        if (diagnostics.isEmpty()) {
            return 0;
        }
        boolean hasError = diagnostics.stream()
                .anyMatch(d -> d.severity() == Severity.ERROR);
        return hasError ? 2 : 1;
    }
}
