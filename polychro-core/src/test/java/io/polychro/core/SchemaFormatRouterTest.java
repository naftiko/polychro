package io.polychro.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.polychro.spi.Document;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SchemaFormatRouterTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void detectShouldReturnDefaultWhenRootIsNull() {
        Document doc = new Document(null, null);
        String result = SchemaFormatRouter.detectSchemaValidator(doc, "json-schema");
        assertEquals("json-schema", result);
    }

    @Test
    void detectShouldReturnDefaultWhenRootIsNotObject() {
        JsonNode arrayNode = MAPPER.createArrayNode();
        Document doc = new Document(arrayNode, null);
        String result = SchemaFormatRouter.detectSchemaValidator(doc, "json-schema");
        assertEquals("json-schema", result);
    }

    @Test
    void detectShouldReturnDefaultWhenNoSchemaField() {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("key", "value");
        Document doc = new Document(root, null);
        String result = SchemaFormatRouter.detectSchemaValidator(doc, "json-schema");
        assertEquals("json-schema", result);
    }

    @Test
    void detectShouldReturnDefaultWhenSchemaIsNotText() {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("$schema", 42);
        Document doc = new Document(root, null);
        String result = SchemaFormatRouter.detectSchemaValidator(doc, "json-schema");
        assertEquals("json-schema", result);
    }

    @Test
    void detectShouldReturnJsonStructureWhenSchemaContainsIndicator() {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("$schema", "https://example.com/json-structure/v1");
        Document doc = new Document(root, null);
        String result = SchemaFormatRouter.detectSchemaValidator(doc, "json-schema");
        assertEquals("json-structure", result);
    }

    @Test
    void detectShouldReturnJsonSchemaWhenSchemaDoesNotContainIndicator() {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("$schema", "https://json-schema.org/draft/2020-12/schema");
        Document doc = new Document(root, null);
        String result = SchemaFormatRouter.detectSchemaValidator(doc, "json-structure");
        assertEquals("json-schema", result);
    }

    @Test
    void detectShouldUseCustomDefault() {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("key", "value");
        Document doc = new Document(root, null);
        String result = SchemaFormatRouter.detectSchemaValidator(doc, "json-structure");
        assertEquals("json-structure", result);
    }
}
