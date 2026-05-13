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
import static org.junit.jupiter.api.Assertions.assertNull;

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
        assertEquals(0, jsonSchema.createCount);
        assertEquals(1, jsonStructure.createCount);
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
        assertEquals(1, jsonStructure.createCount);
    }

    @Test
    void validateShouldReuseCreatedValidatorsAcrossCalls() {
        RecordingFactory jsonSchema = new RecordingFactory("json-schema");
        RecordingFactory jsonStructure = new RecordingFactory("json-structure");
        SchemaModelValidator validator = new SchemaModelValidator(
                jsonSchema,
                new ValidatorConfig(Map.of()),
                jsonStructure,
                new ValidatorConfig(Map.of()),
                "json-schema"
        );

        ObjectNode structureDoc = MAPPER.createObjectNode();
        structureDoc.put("$schema", "https://example.com/json-structure/v1");
        validator.validate(new Document(structureDoc, null));
        validator.validate(new Document(structureDoc, null));

        ObjectNode schemaDoc = MAPPER.createObjectNode();
        schemaDoc.put("name", "test");
        validator.validate(new Document(schemaDoc, null));
        validator.validate(new Document(schemaDoc, null));

        assertEquals(1, jsonSchema.createCount);
        assertEquals(1, jsonStructure.createCount);
    }

    @Test
    void createShouldReturnNullWhenNoSchemaFactoriesAreAvailable() {
        LinterConfig config = new LinterConfig(List.of(), Map.of(), false, "json-schema");

        assertNull(SchemaModelValidator.create(Map.of(), config));
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

    private static class RecordingFactory implements ValidatorFactory {

        private final String name;
        private int createCount;

        private RecordingFactory(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Validator create(ValidatorConfig config) {
            createCount++;
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
}