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
