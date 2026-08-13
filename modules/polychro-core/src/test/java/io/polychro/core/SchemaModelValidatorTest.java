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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.polychro.spi.Diagnostic;
import io.polychro.spi.Document;
import io.polychro.spi.Severity;
import io.polychro.spi.Validator;
import io.polychro.spi.ValidatorConfig;
import io.polychro.spi.ValidatorFactory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SchemaModelValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void validateShouldUseJsonStructureWhenSchemaIndicatesJsonStructure() {
        RecordingFactory jsonSchema = new RecordingFactory("json-schema");
        RecordingFactory jsonStructure = new RecordingFactory("json-structure");
        SchemaModelValidator validator = new SchemaModelValidator(
                jsonSchema,
                new ValidatorConfig(Map.of()),
                jsonStructure,
                new ValidatorConfig(Map.of()),
                "json-schema"
        );

        ObjectNode root = MAPPER.createObjectNode();
        root.put("$schema", "https://example.com/json-structure/v1");
        List<Diagnostic> diagnostics = validator.validate(new Document(root, null));

        assertEquals("json-structure", diagnostics.get(0).code());
    }

    @Test
    void validateShouldUseJsonSchemaWhenSchemaIndicatesJsonSchema() {
        RecordingFactory jsonSchema = new RecordingFactory("json-schema");
        RecordingFactory jsonStructure = new RecordingFactory("json-structure");
        SchemaModelValidator validator = new SchemaModelValidator(
                jsonSchema,
                new ValidatorConfig(Map.of()),
                jsonStructure,
                new ValidatorConfig(Map.of()),
                "json-structure"
        );

        ObjectNode root = MAPPER.createObjectNode();
        root.put("$schema", "https://json-schema.org/draft/2020-12/schema");
        List<Diagnostic> diagnostics = validator.validate(new Document(root, null));

        assertEquals("json-schema", diagnostics.get(0).code());
    }

    @Test
    void validateShouldUseDefaultSchemaValidatorWhenNoSchemaField() {
        RecordingFactory jsonSchema = new RecordingFactory("json-schema");
        RecordingFactory jsonStructure = new RecordingFactory("json-structure");
        SchemaModelValidator validator = new SchemaModelValidator(
                jsonSchema,
                new ValidatorConfig(Map.of()),
                jsonStructure,
                new ValidatorConfig(Map.of()),
                "json-structure"
        );

        ObjectNode root = MAPPER.createObjectNode();
        root.put("name", "test");
        List<Diagnostic> diagnostics = validator.validate(new Document(root, null));

        assertEquals("json-structure", diagnostics.get(0).code());
    }

    @Test
    void validateShouldFallBackToAvailableValidatorWhenPreferredValidatorIsMissing() {
        RecordingFactory jsonSchema = new RecordingFactory("json-schema");
        SchemaModelValidator validator = new SchemaModelValidator(
                jsonSchema,
                new ValidatorConfig(Map.of()),
                null,
                new ValidatorConfig(Map.of()),
                "json-structure"
        );

        ObjectNode root = MAPPER.createObjectNode();
        root.put("name", "test");
        List<Diagnostic> diagnostics = validator.validate(new Document(root, null));

        assertEquals("json-schema", diagnostics.get(0).code());
    }

    @Test
    void validateShouldFallBackToJsonStructureWhenJsonSchemaIsSelectedButMissing() {
        RecordingFactory jsonStructure = new RecordingFactory("json-structure");
        SchemaModelValidator validator = new SchemaModelValidator(
                null,
                new ValidatorConfig(Map.of()),
                jsonStructure,
                new ValidatorConfig(Map.of()),
                "json-schema"
        );

        ObjectNode root = MAPPER.createObjectNode();
        root.put("name", "test");
        List<Diagnostic> diagnostics = validator.validate(new Document(root, null));

        assertEquals("json-structure", diagnostics.get(0).code());
    }

    @Test
    void validateShouldReturnEmptyWhenNoValidatorFactoryIsAvailable() {
        SchemaModelValidator validator = new SchemaModelValidator(
                null,
                new ValidatorConfig(Map.of()),
                null,
                new ValidatorConfig(Map.of()),
                "json-schema"
        );

        ObjectNode root = MAPPER.createObjectNode();
        root.put("name", "test");

        assertEquals(List.of(), validator.validate(new Document(root, null)));
    }

    @Test
    void validateShouldFallBackToJsonStructureWhenJsonSchemaFactoryIsMissing() {
        RecordingFactory jsonStructure = new RecordingFactory("json-structure");
        SchemaModelValidator validator = new SchemaModelValidator(
                null,
                new ValidatorConfig(Map.of()),
                jsonStructure,
                new ValidatorConfig(Map.of()),
                "unsupported"
        );

        ObjectNode root = MAPPER.createObjectNode();
        root.put("name", "test");

        List<Diagnostic> diagnostics = validator.validate(new Document(root, null));

        assertEquals("json-structure", diagnostics.get(0).code());
    }

    @Test
    void selectValidatorNameShouldUseJsonStructureDefaultWhenDefaultIsUnsupportedAndJsonSchemaFactoryMissing() {
        RecordingFactory jsonStructure = new RecordingFactory("json-structure");
        SchemaModelValidator validator = new SchemaModelValidator(
                null,
                new ValidatorConfig(Map.of()),
                jsonStructure,
                new ValidatorConfig(Map.of()),
                "unsupported"
        );

        ObjectNode root = MAPPER.createObjectNode();
        root.put("name", "test");

        assertEquals("json-structure", validator.selectValidatorName(new Document(root, null)));
    }

    @Test
    void selectValidatorNameShouldUseJsonSchemaDefaultWhenDefaultIsUnsupportedAndJsonSchemaFactoryExists() {
        RecordingFactory jsonSchema = new RecordingFactory("json-schema");
        SchemaModelValidator validator = new SchemaModelValidator(
                jsonSchema,
                new ValidatorConfig(Map.of()),
                null,
                new ValidatorConfig(Map.of()),
                "unsupported"
        );

        ObjectNode root = MAPPER.createObjectNode();
        root.put("name", "test");

        assertEquals("json-schema", validator.selectValidatorName(new Document(root, null)));
    }

    // Regression tests for SchemaModelValidator#create silently skipping an auto-discovered
    // (not explicitly configured) json-schema/json-structure factory instead of letting its
    // IllegalArgumentException crash the whole schema-model stage (see LIMITATIONS.md §6 /
    // the .polychro.yml `config: json-schema: ...` case with no `json-structure` block).

    @Test
    void createShouldSilentlySkipAutoDiscoveredJsonStructureFactoryThatFailsWithoutConfig() {
        RequiresSchemaPathFactory jsonSchemaFactory =
                new RequiresSchemaPathFactory(SchemaModelValidator.JSON_SCHEMA_NAME);
        RequiresSchemaPathFactory jsonStructureFactory =
                new RequiresSchemaPathFactory(SchemaModelValidator.JSON_STRUCTURE_NAME);
        Map<String, ValidatorFactory> factories = Map.of(
                SchemaModelValidator.JSON_SCHEMA_NAME, jsonSchemaFactory,
                SchemaModelValidator.JSON_STRUCTURE_NAME, jsonStructureFactory);
        LinterConfig config = new LinterConfig(
                List.of(),
                Map.of(SchemaModelValidator.JSON_SCHEMA_NAME, Map.of("schemaPath", "schema.json")),
                false,
                "json-schema");

        SchemaModelValidator validator = SchemaModelValidator.create(factories, config);

        assertNotNull(validator, "json-schema is explicitly configured, so the validator must be built");
        ObjectNode root = MAPPER.createObjectNode();
        root.put("name", "test");
        List<Diagnostic> diagnostics = validator.validate(new Document(root, null));
        assertEquals(SchemaModelValidator.JSON_SCHEMA_NAME, diagnostics.get(0).code(),
                "the auto-discovered json-structure factory has no config and must fail its own "
                        + "create() — it should be silently dropped rather than crashing, leaving "
                        + "the explicitly-configured json-schema validator as the only one in use");
    }

    @Test
    void createShouldSilentlySkipAutoDiscoveredJsonSchemaFactoryThatFailsWithoutConfig() {
        RequiresSchemaPathFactory jsonSchemaFactory =
                new RequiresSchemaPathFactory(SchemaModelValidator.JSON_SCHEMA_NAME);
        RequiresSchemaPathFactory jsonStructureFactory =
                new RequiresSchemaPathFactory(SchemaModelValidator.JSON_STRUCTURE_NAME);
        Map<String, ValidatorFactory> factories = Map.of(
                SchemaModelValidator.JSON_SCHEMA_NAME, jsonSchemaFactory,
                SchemaModelValidator.JSON_STRUCTURE_NAME, jsonStructureFactory);
        LinterConfig config = new LinterConfig(
                List.of(),
                Map.of(SchemaModelValidator.JSON_STRUCTURE_NAME, Map.of("schemaPath", "schema.json")),
                false,
                "json-structure");

        SchemaModelValidator validator = SchemaModelValidator.create(factories, config);

        assertNotNull(validator, "json-structure is explicitly configured, so the validator must be built");
        ObjectNode root = MAPPER.createObjectNode();
        root.put("name", "test");
        List<Diagnostic> diagnostics = validator.validate(new Document(root, null));
        assertEquals(SchemaModelValidator.JSON_STRUCTURE_NAME, diagnostics.get(0).code(),
                "the auto-discovered json-schema factory has no config and must fail its own "
                        + "create() — it should be silently dropped rather than crashing, leaving "
                        + "the explicitly-configured json-structure validator as the only one in use");
    }

    @Test
    void createShouldReturnNullWhenBothFactoriesAreAutoDiscoveredAndFailWithoutConfig() {
        Map<String, ValidatorFactory> factories = Map.of(
                SchemaModelValidator.JSON_SCHEMA_NAME,
                new RequiresSchemaPathFactory(SchemaModelValidator.JSON_SCHEMA_NAME),
                SchemaModelValidator.JSON_STRUCTURE_NAME,
                new RequiresSchemaPathFactory(SchemaModelValidator.JSON_STRUCTURE_NAME));
        LinterConfig config = new LinterConfig(List.of(), Map.of(), false, "json-schema");

        SchemaModelValidator validator = SchemaModelValidator.create(factories, config);

        assertNull(validator,
                "both factories are auto-discovered, unconfigured, and fail their own create() — "
                        + "the schema-model stage must be dropped entirely rather than returning a "
                        + "validator wrapping nothing");
    }

    @Test
    void createShouldPropagateExceptionWhenExplicitlyConfiguredFactoryFailsToCreate() {
        // Mirrors Linter.Builder#createValidator: a factory the user explicitly configured
        // (present in validatorConfigs, even with an invalid/incomplete config) must still
        // surface its IllegalArgumentException loudly — only auto-discovered, unconfigured
        // factories are silently skipped.
        Map<String, ValidatorFactory> factories = Map.of(
                SchemaModelValidator.JSON_SCHEMA_NAME,
                new RequiresSchemaPathFactory(SchemaModelValidator.JSON_SCHEMA_NAME));
        LinterConfig config = new LinterConfig(
                List.of(),
                Map.of(SchemaModelValidator.JSON_SCHEMA_NAME, Map.of("unrelatedKey", "value")),
                false,
                "json-schema");

        assertThrows(IllegalArgumentException.class, () -> SchemaModelValidator.create(factories, config));
    }

    @Test
    void createShouldInvokeFactoryCreateExactlyOncePerFactory() {
        // Regression test for PR #126 review: create() used to probe each factory with a
        // discardable canCreate() call, then re-create via the constructor on success —
        // invoking factory.create() twice for every successfully-configured factory. For a
        // factory with I/O or schema-compilation side effects, that doubled the cost on every
        // linter startup. Assert the fix: each factory's create() is invoked exactly once.
        CountingFactory jsonSchemaFactory = new CountingFactory(SchemaModelValidator.JSON_SCHEMA_NAME);
        CountingFactory jsonStructureFactory = new CountingFactory(SchemaModelValidator.JSON_STRUCTURE_NAME);
        Map<String, ValidatorFactory> factories = Map.of(
                SchemaModelValidator.JSON_SCHEMA_NAME, jsonSchemaFactory,
                SchemaModelValidator.JSON_STRUCTURE_NAME, jsonStructureFactory);
        LinterConfig config = new LinterConfig(
                List.of(),
                Map.of(
                        SchemaModelValidator.JSON_SCHEMA_NAME, Map.of("schemaPath", "schema.json"),
                        SchemaModelValidator.JSON_STRUCTURE_NAME, Map.of("schemaPath", "schema.json")),
                false,
                "json-schema");

        SchemaModelValidator validator = SchemaModelValidator.create(factories, config);

        assertNotNull(validator);
        assertEquals(1, jsonSchemaFactory.createCallCount);
        assertEquals(1, jsonStructureFactory.createCallCount);
    }

    private static class RecordingFactory implements ValidatorFactory {

        private final String name;

        private RecordingFactory(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Validator create(ValidatorConfig config) {
            return new Validator() {
                @Override
                public String name() {
                    return RecordingFactory.this.name;
                }

                @Override
                public List<Diagnostic> validate(Document doc) {
                    return List.of(new Diagnostic(Severity.INFO, RecordingFactory.this.name,
                            RecordingFactory.this.name, null, null));
                }
            };
        }
    }

    /**
     * Mimics real schema-model {@link ValidatorFactory} implementations (e.g.
     * {@code JsonSchemaValidatorFactory}, {@code JsonStructureValidatorFactory}) that require
     * a {@code schemaPath} configuration key and throw {@link IllegalArgumentException} when
     * it is absent.
     */
    private static class RequiresSchemaPathFactory implements ValidatorFactory {

        private final String name;

        private RequiresSchemaPathFactory(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Validator create(ValidatorConfig config) {
            if (config.get("schemaPath", String.class).isEmpty()) {
                throw new IllegalArgumentException(name + " requires 'schemaPath'");
            }
            return new Validator() {
                @Override
                public String name() {
                    return RequiresSchemaPathFactory.this.name;
                }

                @Override
                public List<Diagnostic> validate(Document doc) {
                    return List.of(new Diagnostic(Severity.INFO, RequiresSchemaPathFactory.this.name,
                            RequiresSchemaPathFactory.this.name, null, null));
                }
            };
        }
    }

    /**
     * Counts how many times {@link #create} is invoked, used to assert the fix for the PR #126
     * review finding that {@code SchemaModelValidator#create} used to invoke a factory's
     * {@code create()} twice (once to probe, once to build) for every successfully-configured
     * factory.
     */
    private static class CountingFactory implements ValidatorFactory {

        private final String name;
        int createCallCount;

        private CountingFactory(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Validator create(ValidatorConfig config) {
            createCallCount++;
            return new Validator() {
                @Override
                public String name() {
                    return CountingFactory.this.name;
                }

                @Override
                public List<Diagnostic> validate(Document doc) {
                    return List.of(new Diagnostic(Severity.INFO, CountingFactory.this.name,
                            CountingFactory.this.name, null, null));
                }
            };
        }
    }
}
