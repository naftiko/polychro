package io.polychro.mcp;

import io.naftiko.engine.NaftikoEngine;
import io.polychro.core.Linter;
import io.polychro.core.LinterConfig;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Wires the Polychro linting engine to a Naftiko MCP server.
 * <p>
 * The server exposes four tools: lint, validate-schema, list-rules, and explain-diagnostic.
 * Step handlers override the placeholder HTTP calls declared in the capability YAML.
 */
public class PolychroMcpServer {

    private final NaftikoEngine engine;

    PolychroMcpServer(NaftikoEngine engine) {
        this.engine = engine;
    }

    /**
     * Start the MCP server (blocking — runs until stopped).
     */
    public void start() throws Exception {
        engine.start();
    }

    /**
     * Stop the MCP server gracefully.
     */
    public void stop() throws Exception {
        engine.stop();
    }

    NaftikoEngine engine() {
        return engine;
    }

    /**
     * Create a new builder for configuring the MCP server.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing a {@link PolychroMcpServer}.
     */
    public static class Builder {

        private LinterConfig linterConfig;
        private Path configPath;
        private final Map<String, ExplainDiagnosticHandler.RuleExplanation> explanations =
                new LinkedHashMap<>();

        Builder() {
        }

        /**
         * Set the linter configuration directly.
         */
        public Builder config(LinterConfig config) {
            this.linterConfig = config;
            this.configPath = null;
            return this;
        }

        /**
         * Load the linter configuration from a file path.
         */
        public Builder config(Path path) {
            this.configPath = path;
            this.linterConfig = null;
            return this;
        }

        /**
         * Register an explanation for a diagnostic code.
         */
        public Builder explanation(String code, String explanation, String suggestion) {
            explanations.put(code, new ExplainDiagnosticHandler.RuleExplanation(
                    explanation, suggestion));
            return this;
        }

        /**
         * Build and return the server (does not start it).
         */
        public PolychroMcpServer build() {
            LinterConfig effectiveConfig = resolveConfig();
            Linter linter = Linter.builder().config(effectiveConfig).build();

            NaftikoEngine engine = NaftikoEngine.builder()
                    .capabilityFromClasspath("/polychro-capability.yml")
                    .stepHandler("do-lint", new LintHandler(linter))
                    .stepHandler("do-validate-schema", new ValidateSchemaHandler(effectiveConfig))
                    .stepHandler("do-list-rules", new ListRulesHandler(linter))
                    .stepHandler("do-explain", new ExplainDiagnosticHandler(explanations))
                    .build();

            return new PolychroMcpServer(engine);
        }

        LinterConfig resolveConfig() {
            if (linterConfig != null) {
                return linterConfig;
            }
            if (configPath != null) {
                return LinterConfig.load(configPath);
            }
            return LinterConfig.defaults();
        }
    }
}
