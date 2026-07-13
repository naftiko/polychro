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
import io.polychro.spi.Document;
import io.polychro.spi.Severity;
import io.polychro.spi.Validator;
import io.polychro.spi.ValidatorConfig;
import io.polychro.spi.ValidatorFactory;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * The core orchestrator that discovers validators via {@link ServiceLoader},
 * runs them in order, and merges diagnostics.
 */
public class Linter {

    private final List<Validator> validators;
    private final boolean failFast;

    Linter(List<Validator> validators, boolean failFast) {
        this.validators = List.copyOf(validators);
        this.failFast = failFast;
    }

    /**
     * Lint a document through all configured validators.
     *
     * @param doc the document to validate
     * @return deduplicated, sorted diagnostics from all validators
     */
    public List<Diagnostic> lint(Document doc) {
        List<Diagnostic> allDiagnostics = new ArrayList<>();

        for (Validator validator : validators) {
            List<Diagnostic> results = validator.validate(doc);
            allDiagnostics.addAll(results);

            if (failFast && results.stream().anyMatch(d -> d.severity() == Severity.ERROR)) {
                break;
            }
        }

        List<Diagnostic> deduplicated = DiagnosticDeduplicator.deduplicate(allDiagnostics);
        deduplicated.sort(null);
        return deduplicated;
    }

    /**
     * @return the ordered list of validators in this linter
     */
    public List<Validator> validators() {
        return validators;
    }

    /**
     * Create a new builder for configuring a Linter.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing a {@link Linter} with configuration.
     */
    public static class Builder {

        private LinterConfig config;
        private final List<Validator> additionalValidators = new ArrayList<>();
        private Iterable<ValidatorFactory> factorySource;

        Builder() {
        }

        /**
         * Load configuration from a file path.
         */
        public Builder config(Path path) {
            this.config = LinterConfig.load(path);
            return this;
        }

        /**
         * Load configuration from an input stream.
         */
        public Builder config(InputStream is) {
            this.config = LinterConfig.load(is);
            return this;
        }

        /**
         * Use an already-parsed configuration.
         */
        public Builder config(LinterConfig config) {
            this.config = config;
            return this;
        }

        /**
         * Add a validator programmatically (in addition to discovered ones).
         */
        public Builder addValidator(Validator validator) {
            this.additionalValidators.add(validator);
            return this;
        }

        /**
         * Override the factory source (for testing).
         */
        Builder factories(Iterable<ValidatorFactory> factories) {
            this.factorySource = factories;
            return this;
        }

        /**
         * Build the Linter, discovering validators via ServiceLoader and applying config.
         */
        public Linter build() {
            if (config == null) {
                config = LinterConfig.defaults();
            }

            Map<String, ValidatorFactory> discoveredFactories = discoverFactories();
            List<Validator> orderedValidators = buildValidatorList(discoveredFactories);

            return new Linter(orderedValidators, config.failFast());
        }

        private Map<String, ValidatorFactory> discoverFactories() {
            Iterable<ValidatorFactory> source = factorySource != null
                    ? factorySource
                    : ServiceLoader.load(ValidatorFactory.class);

            Map<String, ValidatorFactory> factories = new LinkedHashMap<>();
            for (ValidatorFactory factory : source) {
                factories.put(factory.name(), factory);
            }
            return factories;
        }

        private List<Validator> buildValidatorList(Map<String, ValidatorFactory> factories) {
            List<Validator> validators = new ArrayList<>();

            List<String> requestedNames = config.validators();
            boolean autoDiscovered = requestedNames.isEmpty();
            if (autoDiscovered) {
                // Use all discovered factories
                requestedNames = new ArrayList<>(factories.keySet());
            }

            boolean useImplicitSchemaModel = config.validators().isEmpty() && hasSchemaModelConfig();
            boolean schemaModelAdded = false;

            for (String name : requestedNames) {
                if (SchemaModelValidator.NAME.equals(name)
                        || (useImplicitSchemaModel && SchemaModelValidator.isSchemaValidatorName(name))) {
                    if (!schemaModelAdded) {
                        SchemaModelValidator schemaModelValidator = SchemaModelValidator.create(factories, config);
                        if (schemaModelValidator != null) {
                            validators.add(schemaModelValidator);
                        }
                        schemaModelAdded = true;
                    }
                    continue;
                }

                ValidatorFactory factory = factories.get(name);
                if (factory != null) {
                    Map<String, Object> props = config.validatorConfigs()
                            .getOrDefault(name, Map.of());
                    boolean hasExplicitConfig = config.validatorConfigs().containsKey(name);
                    Validator created = createValidator(factory, props, autoDiscovered, hasExplicitConfig);
                    if (created != null) {
                        validators.add(wrapWithFormatGate(created, factory.supportedFormats()));
                    }
                }
            }

            validators.addAll(additionalValidators);
            return validators;
        }

        /**
         * Instantiate a validator from a factory. When the linter is auto-discovering
         * factories (no explicit {@code validators:} list in config) and the user has
         * not supplied a config block for this factory, missing-config exceptions
         * thrown by {@link ValidatorFactory#create(ValidatorConfig)} are swallowed
         * and the factory is silently skipped. This lets a CLI invocation like
         * {@code polychro lint --ruleset foo.yml file.yml} succeed even though
         * other auto-discovered factories (json-schema, json-structure, ...)
         * have no configuration. When the user explicitly requested a validator
         * (either by name in {@code validators:} or by providing a config block
         * for it), the exception is propagated so misconfiguration is loud.
         */
        private Validator createValidator(ValidatorFactory factory, Map<String, Object> props,
                                          boolean autoDiscovered, boolean hasExplicitConfig) {
            try {
                return factory.create(new ValidatorConfig(props));
            } catch (IllegalArgumentException e) {
                if (autoDiscovered && !hasExplicitConfig) {
                    // Factory requires configuration the user did not provide.
                    // Skip silently rather than crash the linter on a flag combination
                    // the user did not ask for.
                    return null;
                }
                throw e;
            }
        }

        /**
         * Wrap a validator so that it only runs against documents whose
         * {@link Document#format() format} matches the factory's
         * {@link ValidatorFactory#supportedFormats() supportedFormats}.
         * <p>
         * A factory that declares an empty set is treated as unconstrained:
         * the validator will run on every document. This preserves the
         * existing behaviour for validators that genuinely apply to all
         * formats (e.g. {@code wellformedness}) while preventing
         * format-specific validators (e.g. {@code markdown}) from emitting
         * spurious diagnostics on unrelated documents — see issue #20.
         */
        private static Validator wrapWithFormatGate(Validator delegate, Set<String> supportedFormats) {
            if (supportedFormats == null || supportedFormats.isEmpty()) {
                return delegate;
            }
            return new FormatGatedValidator(delegate, Set.copyOf(supportedFormats));
        }

        private boolean hasSchemaModelConfig() {
            return config.validatorConfigs().containsKey(SchemaModelValidator.JSON_SCHEMA_NAME)
                    || config.validatorConfigs().containsKey(SchemaModelValidator.JSON_STRUCTURE_NAME);
        }
    }
}
