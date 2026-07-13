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
package io.polychro.jsonstructure;

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
