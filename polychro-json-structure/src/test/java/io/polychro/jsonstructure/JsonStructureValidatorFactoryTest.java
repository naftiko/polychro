package io.polychro.jsonstructure;

import com.fasterxml.jackson.databind.JsonNode;
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

class JsonStructureValidatorFactoryTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void nameShouldReturnJsonStructure() {
        assertEquals("json-structure", new JsonStructureValidatorFactory().name());
    }

    @Test
    void createShouldBuildSchemaValidatorWhenNoSchemaProvided() {
        ValidatorConfig config = new ValidatorConfig(Map.of("mode", "schema"));
        Validator validator = new JsonStructureValidatorFactory().create(config);

        assertNotNull(validator);
        assertEquals("json-structure", validator.name());
    }

    @Test
    void createShouldBuildInstanceValidatorWithInlineSchema() {
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.put("$schema", "https://json-structure.org/meta/core/v0/#");
        schemaNode.put("$id", "https://example.com/test");
        schemaNode.put("name", "Test");
        schemaNode.put("type", "object");

        ValidatorConfig config = new ValidatorConfig(Map.of("schemaNode", schemaNode));
        Validator validator = new JsonStructureValidatorFactory().create(config);

        assertNotNull(validator);
    }

    @Test
    void createShouldBuildInstanceValidatorFromFilesystemPath(@TempDir Path tempDir) throws IOException {
        String schemaContent = """
                {
                  "$schema": "https://json-structure.org/meta/core/v0/#",
                  "$id": "https://example.com/fs-test",
                  "name": "FsTest",
                  "type": "object",
                  "properties": { "x": { "type": "string" } }
                }
                """;
        Path schemaFile = tempDir.resolve("schema.json");
        Files.writeString(schemaFile, schemaContent);

        ValidatorConfig config = new ValidatorConfig(Map.of("schemaPath", schemaFile.toString()));
        Validator validator = new JsonStructureValidatorFactory().create(config);

        assertNotNull(validator);
    }

    @Test
    void createShouldBuildInstanceValidatorFromClasspathResource() {
        ValidatorConfig config = new ValidatorConfig(Map.of("schemaPath", "schemas/person-schema.json"));
        Validator validator = new JsonStructureValidatorFactory().create(config);

        assertNotNull(validator);
    }

    @Test
    void createShouldDefaultToSchemaModeWhenNoSchemaConfig() {
        ValidatorConfig config = new ValidatorConfig(Map.of());
        Validator validator = new JsonStructureValidatorFactory().create(config);

        assertNotNull(validator);
        assertInstanceOf(JsonStructureValidator.class, validator);
    }

    @Test
    void createShouldThrowWhenSchemaPathNotFound() {
        ValidatorConfig config = new ValidatorConfig(Map.of("schemaPath", "nonexistent/path.json"));

        assertThrows(IllegalArgumentException.class,
                () -> new JsonStructureValidatorFactory().create(config));
    }

    @Test
    void createShouldThrowWhenSchemaFileIsInvalid(@TempDir Path tempDir) throws IOException {
        Path schemaFile = tempDir.resolve("bad.json");
        Files.writeString(schemaFile, "not valid json {{{");

        ValidatorConfig config = new ValidatorConfig(Map.of("schemaPath", schemaFile.toString()));

        assertThrows(IllegalArgumentException.class,
                () -> new JsonStructureValidatorFactory().create(config));
    }

    @Test
    void createShouldThrowWhenClasspathResourceIsInvalidJson() {
        ValidatorConfig config = new ValidatorConfig(Map.of("schemaPath", "schemas/invalid-json.txt"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new JsonStructureValidatorFactory().create(config));
        assertTrue(ex.getMessage().contains("Failed to read schema from classpath"));
    }

    @Test
    void createShouldRespectStrictFormatValidation() {
        ValidatorConfig config = new ValidatorConfig(Map.of(
                "mode", "schema",
                "strictFormatValidation", true));
        Validator validator = new JsonStructureValidatorFactory().create(config);

        assertNotNull(validator);
    }

    @Test
    void createShouldRespectAllowImport() {
        ValidatorConfig config = new ValidatorConfig(Map.of(
                "mode", "schema",
                "allowImport", true));
        Validator validator = new JsonStructureValidatorFactory().create(config);

        assertNotNull(validator);
    }

    @Test
    void createShouldRespectExternalSchemas() {
        ObjectNode extSchema = MAPPER.createObjectNode();
        extSchema.put("type", "string");
        Map<String, JsonNode> external = Map.of("https://example.com/ext", extSchema);

        ValidatorConfig config = new ValidatorConfig(Map.of(
                "mode", "schema",
                "externalSchemas", external));
        Validator validator = new JsonStructureValidatorFactory().create(config);

        assertNotNull(validator);
    }

    @Test
    void createShouldDefaultToInstanceModeWhenModeIsUnrecognized() {
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.put("type", "object");

        ValidatorConfig config = new ValidatorConfig(Map.of(
                "schemaNode", schemaNode,
                "mode", "unknown"));
        Validator validator = new JsonStructureValidatorFactory().create(config);

        assertNotNull(validator);
    }

    @Test
    void serviceLoaderShouldDiscoverFactory() {
        ServiceLoader<ValidatorFactory> loader = ServiceLoader.load(ValidatorFactory.class);

        boolean found = false;
        for (ValidatorFactory f : loader) {
            if ("json-structure".equals(f.name())) {
                found = true;
                break;
            }
        }
        assertTrue(found, "JsonStructureValidatorFactory should be discoverable via ServiceLoader");
    }
}
