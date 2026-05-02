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
package io.polychro.jsonschema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.polychro.spi.Validator;
import io.polychro.spi.ValidatorConfig;
import io.polychro.spi.ValidatorFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.*;

class JsonSchemaValidatorFactoryTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void nameShouldReturnJsonSchema() {
        JsonSchemaValidatorFactory factory = new JsonSchemaValidatorFactory();

        assertEquals("json-schema", factory.name());
    }

    @Test
    void createShouldBuildValidatorFromInlineSchemaNode() {
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.put("type", "object");

        ValidatorConfig config = new ValidatorConfig(Map.of("schemaNode", schemaNode));
        JsonSchemaValidatorFactory factory = new JsonSchemaValidatorFactory();

        Validator validator = factory.create(config);

        assertNotNull(validator);
        assertEquals("json-schema", validator.name());
    }

    @Test
    void createShouldBuildValidatorFromFilesystemPath(@TempDir Path tempDir) throws IOException {
        String schemaContent = """
                {
                  "type": "object",
                  "properties": { "x": { "type": "string" } }
                }
                """;
        Path schemaFile = tempDir.resolve("schema.json");
        Files.writeString(schemaFile, schemaContent);

        ValidatorConfig config = new ValidatorConfig(Map.of("schemaPath", schemaFile.toString()));
        JsonSchemaValidatorFactory factory = new JsonSchemaValidatorFactory();

        Validator validator = factory.create(config);

        assertNotNull(validator);
    }

    @Test
    void createShouldBuildValidatorFromClasspathResource() {
        ValidatorConfig config = new ValidatorConfig(Map.of("schemaPath", "schemas/person-schema.json"));
        JsonSchemaValidatorFactory factory = new JsonSchemaValidatorFactory();

        Validator validator = factory.create(config);

        assertNotNull(validator);
    }

    @Test
    void createShouldThrowWhenNoSchemaConfigured() {
        ValidatorConfig config = new ValidatorConfig(Map.of());
        JsonSchemaValidatorFactory factory = new JsonSchemaValidatorFactory();

        assertThrows(IllegalArgumentException.class, () -> factory.create(config));
    }

    @Test
    void createShouldThrowWhenSchemaPathNotFound() {
        ValidatorConfig config = new ValidatorConfig(Map.of("schemaPath", "nonexistent/path.json"));
        JsonSchemaValidatorFactory factory = new JsonSchemaValidatorFactory();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> factory.create(config));
        assertTrue(ex.getMessage().contains("nonexistent/path.json"));
    }

    @Test
    void createShouldThrowWhenSchemaFileIsInvalid(@TempDir Path tempDir) throws IOException {
        Path schemaFile = tempDir.resolve("bad.json");
        Files.writeString(schemaFile, "not valid json {{{");

        ValidatorConfig config = new ValidatorConfig(Map.of("schemaPath", schemaFile.toString()));
        JsonSchemaValidatorFactory factory = new JsonSchemaValidatorFactory();

        assertThrows(IllegalArgumentException.class, () -> factory.create(config));
    }

    @Test
    void serviceLoaderShouldDiscoverFactory() {
        ServiceLoader<ValidatorFactory> loader = ServiceLoader.load(ValidatorFactory.class);

        boolean found = false;
        for (ValidatorFactory f : loader) {
            if ("json-schema".equals(f.name())) {
                found = true;
                break;
            }
        }
        assertTrue(found, "JsonSchemaValidatorFactory should be discoverable via ServiceLoader");
    }

    @Test
    void createShouldAutoDetectDraftFromInlineSchema() {
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.put("$schema", "http://json-schema.org/draft-07/schema#");
        schemaNode.put("type", "object");

        ValidatorConfig config = new ValidatorConfig(Map.of("schemaNode", schemaNode));
        JsonSchemaValidatorFactory factory = new JsonSchemaValidatorFactory();

        Validator validator = factory.create(config);

        assertNotNull(validator);
    }

    @Test
    void createShouldThrowWhenClasspathResourceIsInvalidJson() {
        ValidatorConfig config = new ValidatorConfig(Map.of("schemaPath", "schemas/invalid-json-schema.txt"));
        JsonSchemaValidatorFactory factory = new JsonSchemaValidatorFactory();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> factory.create(config));
        assertTrue(ex.getMessage().contains("Failed to read schema from classpath"));
    }
}
