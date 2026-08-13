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
import io.polychro.spi.Validator;
import io.polychro.spi.ValidatorConfig;
import io.polychro.spi.ValidatorFactory;

import java.util.List;
import java.util.Map;

class SchemaModelValidator implements Validator {

    static final String NAME = "schema-model";
    static final String JSON_SCHEMA_NAME = "json-schema";
    static final String JSON_STRUCTURE_NAME = "json-structure";

    private final String defaultSchemaValidator;
    private final Validator jsonSchemaValidator;
    private final Validator jsonStructureValidator;

    SchemaModelValidator(
            ValidatorFactory jsonSchemaFactory,
            ValidatorConfig jsonSchemaConfig,
            ValidatorFactory jsonStructureFactory,
            ValidatorConfig jsonStructureConfig,
            String defaultSchemaValidator) {
        this(
                jsonSchemaFactory != null ? jsonSchemaFactory.create(jsonSchemaConfig) : null,
                jsonStructureFactory != null ? jsonStructureFactory.create(jsonStructureConfig) : null,
                defaultSchemaValidator);
    }

    /**
     * Build directly from already-created {@link Validator} instances, used by
     * {@link #create(Map, LinterConfig)} so that each factory's {@code create()} is invoked
     * at most once — see {@link #buildValidator}.
     */
    private SchemaModelValidator(
            Validator jsonSchemaValidator, Validator jsonStructureValidator, String defaultSchemaValidator) {
        this.jsonSchemaValidator = jsonSchemaValidator;
        this.jsonStructureValidator = jsonStructureValidator;
        this.defaultSchemaValidator = defaultSchemaValidator;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public List<Diagnostic> validate(Document doc) {
        Validator validator = resolveValidator(doc);
        if (validator == null) {
            return List.of();
        }
        return validator.validate(doc);
    }

    String selectValidatorName(Document doc) {
        return SchemaFormatRouter.detectSchemaValidator(doc, defaultValidatorName());
    }

    private Validator resolveValidator(Document doc) {
        String selectedName = selectValidatorName(doc);
        if (JSON_STRUCTURE_NAME.equals(selectedName)) {
            if (jsonStructureValidator != null) {
                return jsonStructureValidator;
            }
            return jsonSchemaValidator;
        }

        if (jsonSchemaValidator != null) {
            return jsonSchemaValidator;
        }
        return jsonStructureValidator;
    }

    private String defaultValidatorName() {
        if (JSON_STRUCTURE_NAME.equals(defaultSchemaValidator) || JSON_SCHEMA_NAME.equals(defaultSchemaValidator)) {
            return defaultSchemaValidator;
        }
        // Unrecognised configured value (or null) — prefer json-schema when its factory
        // is available, otherwise fall back to json-structure. This keeps the validator
        // usable instead of failing hard on misconfiguration; callers that need strict
        // behaviour should validate the LinterConfig before constructing the linter.
        if (jsonSchemaValidator != null) {
            return JSON_SCHEMA_NAME;
        }
        return JSON_STRUCTURE_NAME;
    }

    static boolean isSchemaValidatorName(String name) {
        return JSON_SCHEMA_NAME.equals(name) || JSON_STRUCTURE_NAME.equals(name);
    }

    static SchemaModelValidator create(Map<String, ValidatorFactory> factories, LinterConfig config) {
        ValidatorFactory jsonSchemaFactory = factories.get(JSON_SCHEMA_NAME);
        ValidatorFactory jsonStructureFactory = factories.get(JSON_STRUCTURE_NAME);
        if (jsonSchemaFactory == null && jsonStructureFactory == null) {
            return null;
        }

        Map<String, Map<String, Object>> validatorConfigs = config.validatorConfigs();
        boolean jsonSchemaExplicit = validatorConfigs.containsKey(JSON_SCHEMA_NAME);
        boolean jsonStructureExplicit = validatorConfigs.containsKey(JSON_STRUCTURE_NAME);
        ValidatorConfig jsonSchemaConfig = new ValidatorConfig(
                validatorConfigs.getOrDefault(JSON_SCHEMA_NAME, Map.of()));
        ValidatorConfig jsonStructureConfig = new ValidatorConfig(
                validatorConfigs.getOrDefault(JSON_STRUCTURE_NAME, Map.of()));

        // A .polychro.yml (or --schema/--config flag combination) commonly configures only
        // ONE of the two schema-model factories (e.g. `config: json-schema: schemaPath: ...`
        // with no `json-structure` block at all). Without this guard, the factory that has no
        // configuration throws IllegalArgumentException from its own create() (it requires
        // schemaNode/schemaPath/mode) and that exception propagated uncaught out of this
        // constructor, crashing the whole `lint` invocation even though the *other* schema
        // validator was perfectly configured and ready to run. Mirror the same
        // autoDiscovered-and-not-explicitly-configured "silent skip" semantics already applied
        // to every other validator factory in Linter.Builder#createValidator (see issue #20):
        // only silently drop a factory when the user did not explicitly configure it.
        Validator jsonSchemaValidator = buildValidator(jsonSchemaFactory, jsonSchemaConfig, jsonSchemaExplicit);
        Validator jsonStructureValidator =
                buildValidator(jsonStructureFactory, jsonStructureConfig, jsonStructureExplicit);
        if (jsonSchemaValidator == null && jsonStructureValidator == null) {
            return null;
        }

        return new SchemaModelValidator(jsonSchemaValidator, jsonStructureValidator, config.defaultSchemaValidator());
    }

    /**
     * Build the {@link Validator} for a single schema-model factory, invoking
     * {@link ValidatorFactory#create} at most once — see the "canCreate probing twice" issue in
     * PR #126 review: probing with a discardable call and re-creating on success doubled the
     * cost of any factory with I/O or compilation side effects on every linter startup.
     * <p>
     * Returns {@code null} when {@code factory} is {@code null} (not discovered). When the
     * factory was not explicitly configured by the user and {@code create()} throws
     * {@link IllegalArgumentException} (missing required config), the failure is swallowed and
     * {@code null} is returned — mirroring the auto-discovered "silent skip" semantics in
     * {@link Linter.Builder#createValidator} (issue #20). When the factory WAS explicitly
     * configured, any {@link IllegalArgumentException} propagates so misconfiguration is loud.
     */
    private static Validator buildValidator(ValidatorFactory factory, ValidatorConfig config, boolean explicit) {
        if (factory == null) {
            return null;
        }
        if (explicit) {
            // Explicitly configured by the user — let IllegalArgumentException propagate so
            // misconfiguration is loud (mirrors Linter.Builder#createValidator, issue #20).
            return factory.create(config);
        }
        try {
            return factory.create(config);
        } catch (IllegalArgumentException e) {
            // Auto-discovered, not explicitly configured — silently drop it (issue #20).
            return null;
        }
    }
}
