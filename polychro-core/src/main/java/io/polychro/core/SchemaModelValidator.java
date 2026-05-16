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

    private final ValidatorFactory jsonSchemaFactory;
    private final ValidatorConfig jsonSchemaConfig;
    private final ValidatorFactory jsonStructureFactory;
    private final ValidatorConfig jsonStructureConfig;
    private final String defaultSchemaValidator;

    private volatile Validator jsonSchemaValidator;
    private volatile Validator jsonStructureValidator;

    SchemaModelValidator(
            ValidatorFactory jsonSchemaFactory,
            ValidatorConfig jsonSchemaConfig,
            ValidatorFactory jsonStructureFactory,
            ValidatorConfig jsonStructureConfig,
            String defaultSchemaValidator) {
        this.jsonSchemaFactory = jsonSchemaFactory;
        this.jsonSchemaConfig = jsonSchemaConfig;
        this.jsonStructureFactory = jsonStructureFactory;
        this.jsonStructureConfig = jsonStructureConfig;
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
            Validator validator = jsonStructureValidator();
            if (validator != null) {
                return validator;
            }
            return jsonSchemaValidator();
        }

        Validator validator = jsonSchemaValidator();
        if (validator != null) {
            return validator;
        }
        return jsonStructureValidator();
    }

    private String defaultValidatorName() {
        if (JSON_STRUCTURE_NAME.equals(defaultSchemaValidator) || JSON_SCHEMA_NAME.equals(defaultSchemaValidator)) {
            return defaultSchemaValidator;
        }
        if (jsonSchemaFactory != null) {
            return JSON_SCHEMA_NAME;
        }
        return JSON_STRUCTURE_NAME;
    }

    private Validator jsonSchemaValidator() {
        if (jsonSchemaFactory == null) {
            return null;
        }
        Validator local = jsonSchemaValidator;
        if (local == null) {
            synchronized (this) {
                local = jsonSchemaValidator;
                if (local == null) {
                    local = jsonSchemaFactory.create(jsonSchemaConfig);
                    jsonSchemaValidator = local;
                }
            }
        }
        return local;
    }

    private Validator jsonStructureValidator() {
        if (jsonStructureFactory == null) {
            return null;
        }
        Validator local = jsonStructureValidator;
        if (local == null) {
            synchronized (this) {
                local = jsonStructureValidator;
                if (local == null) {
                    local = jsonStructureFactory.create(jsonStructureConfig);
                    jsonStructureValidator = local;
                }
            }
        }
        return local;
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
        ValidatorConfig jsonSchemaConfig = new ValidatorConfig(
                validatorConfigs.getOrDefault(JSON_SCHEMA_NAME, Map.of()));
        ValidatorConfig jsonStructureConfig = new ValidatorConfig(
                validatorConfigs.getOrDefault(JSON_STRUCTURE_NAME, Map.of()));

        return new SchemaModelValidator(
                jsonSchemaFactory,
                jsonSchemaConfig,
                jsonStructureFactory,
                jsonStructureConfig,
                config.defaultSchemaValidator());
    }
}
