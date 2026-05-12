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
 */package io.polychro.capability;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.naftiko.Capability;
import io.naftiko.engine.step.StepHandlerRegistry;
import io.naftiko.spec.NaftikoSpec;
import io.polychro.core.Linter;
import io.polychro.core.LinterConfig;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Polychro linting capability exposed as an MCP server via Naftiko Framework.
 * <p>
 * Extends {@link Capability} directly — exposes four tools: lint, validate-schema,
 * list-rules, and explain-diagnostic. Step handlers override the placeholder HTTP calls
 * declared in the capability YAML.
 */
public class PolychroCapability extends Capability {

    PolychroCapability(NaftikoSpec spec, StepHandlerRegistry registry) throws Exception {
        super(spec);
        setStepHandlerRegistry(registry);
    }

    /**
     * Create a new builder for configuring the Polychro MCP capability.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing a {@link PolychroCapability}.
     */
    public static class Builder extends Capability.Builder {

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
         * Build and return the capability (does not start it).
         */
        public PolychroCapability build() {
            LinterConfig effectiveConfig = resolveConfig();
            Linter linter = Linter.builder().config(effectiveConfig).build();

            NaftikoSpec spec = loadSpec();
            StepHandlerRegistry registry = new StepHandlerRegistry();
            registry.register("do-lint", new LintHandler(linter));
            registry.register("do-validate-schema", new ValidateSchemaHandler(effectiveConfig));
            registry.register("do-list-rules", new ListRulesHandler(linter));
            registry.register("do-explain", new ExplainDiagnosticHandler(explanations));

            try {
                return new PolychroCapability(spec, registry);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to initialize PolychroCapability", e);
            }
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

        private NaftikoSpec loadSpec() {
            try (InputStream is = getClass().getResourceAsStream("/polychro-capability.yml")) {
                if (is == null) {
                    throw new IllegalStateException(
                            "Classpath resource not found: /polychro-capability.yml");
                }
                ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                return mapper.readValue(is, NaftikoSpec.class);
            } catch (IllegalStateException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException(
                        "Failed to load polychro-capability.yml from classpath", e);
            }
        }
    }
}
