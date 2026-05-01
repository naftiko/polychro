package io.polychro.jsonstructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonStructureAutoDetectionTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void isJsonStructureDocumentShouldReturnTrueForJsonStructureSchema() {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("$schema", "https://json-structure.org/meta/core/v0/#");

        assertTrue(JsonStructureValidator.isJsonStructureDocument(root));
    }

    @Test
    void isJsonStructureDocumentShouldReturnFalseForJsonSchema() {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("$schema", "https://json-schema.org/draft/2020-12/schema");

        assertFalse(JsonStructureValidator.isJsonStructureDocument(root));
    }

    @Test
    void isJsonStructureDocumentShouldReturnFalseWhenNoSchemaField() {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("type", "object");

        assertFalse(JsonStructureValidator.isJsonStructureDocument(root));
    }

    @Test
    void isJsonStructureDocumentShouldReturnFalseForNull() {
        assertFalse(JsonStructureValidator.isJsonStructureDocument(null));
    }

    @Test
    void isJsonStructureDocumentShouldReturnTrueForAnyJsonStructureMetaPath() {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("$schema", "https://json-structure.org/meta/validation/v0/#");

        assertTrue(JsonStructureValidator.isJsonStructureDocument(root));
    }
}
