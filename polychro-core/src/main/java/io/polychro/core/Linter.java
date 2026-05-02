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
            if (requestedNames.isEmpty()) {
                // Use all discovered factories
                requestedNames = new ArrayList<>(factories.keySet());
            }

            for (String name : requestedNames) {
                ValidatorFactory factory = factories.get(name);
                if (factory != null) {
                    Map<String, Object> props = config.validatorConfigs()
                            .getOrDefault(name, Map.of());
                    validators.add(factory.create(new ValidatorConfig(props)));
                }
            }

            validators.addAll(additionalValidators);
            return validators;
        }
    }
}
