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
import com.networknt.schema.JsonSchema;
import com.networknt.schema.SpecVersion;
import io.polychro.spi.Diagnostic;
import io.polychro.spi.Document;
import io.polychro.spi.Severity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonSchemaDiagnosticMappingTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void toDiagnosticShouldMapPathFromInstanceLocation() {
        String schemaJson = """
                {
                  "type": "object",
                  "properties": {
                    "count": { "type": "integer" }
                  }
                }
                """;
        JsonSchemaValidator v = buildValidator(schemaJson);
        ObjectNode root = MAPPER.createObjectNode();
        root.put("count", "not-an-int");
        Document doc = new Document(root, "test.json");

        List<Diagnostic> result = v.validate(doc);

        assertFalse(result.isEmpty());
        Diagnostic d = result.get(0);
        assertNotNull(d.path());
        assertTrue(d.path().contains("count"));
    }

    @Test
    void toDiagnosticShouldSetSeverityToError() {
        String schemaJson = """
                {
                  "type": "object",
                  "properties": {
                    "x": { "type": "string" }
                  },
                  "required": ["x"]
                }
                """;
        JsonSchemaValidator v = buildValidator(schemaJson);
        ObjectNode root = MAPPER.createObjectNode();
        Document doc = new Document(root, "test.json");

        List<Diagnostic> result = v.validate(doc);

        assertFalse(result.isEmpty());
        result.forEach(d -> assertEquals(Severity.ERROR, d.severity()));
    }

    @Test
    void toDiagnosticShouldIncludeMessageText() {
        String schemaJson = """
                {
                  "type": "object",
                  "required": ["name"]
                }
                """;
        JsonSchemaValidator v = buildValidator(schemaJson);
        ObjectNode root = MAPPER.createObjectNode();
        Document doc = new Document(root, "test.json");

        List<Diagnostic> result = v.validate(doc);

        assertFalse(result.isEmpty());
        assertNotNull(result.get(0).message());
        assertFalse(result.get(0).message().isBlank());
    }

    @Test
    void toDiagnosticShouldSetRangeToNull() {
        String schemaJson = """
                {
                  "type": "object",
                  "required": ["a"]
                }
                """;
        JsonSchemaValidator v = buildValidator(schemaJson);
        ObjectNode root = MAPPER.createObjectNode();
        Document doc = new Document(root, "test.json");

        List<Diagnostic> result = v.validate(doc);

        assertFalse(result.isEmpty());
        assertNull(result.get(0).range());
    }

    @Test
    void toDiagnosticShouldSetCodeFromValidationType() {
        String schemaJson = """
                {
                  "type": "object",
                  "required": ["field"]
                }
                """;
        JsonSchemaValidator v = buildValidator(schemaJson);
        ObjectNode root = MAPPER.createObjectNode();
        Document doc = new Document(root, "test.json");

        List<Diagnostic> result = v.validate(doc);

        assertFalse(result.isEmpty());
        assertNotNull(result.get(0).code());
    }

    @Test
    void toDiagnosticShouldHandleRootLevelPath() {
        String schemaJson = """
                {
                  "type": "array"
                }
                """;
        JsonSchemaValidator v = buildValidator(schemaJson);
        ObjectNode root = MAPPER.createObjectNode();
        Document doc = new Document(root, "test.json");

        List<Diagnostic> result = v.validate(doc);

        assertFalse(result.isEmpty());
        // Root-level path should be "$" or empty
        assertNotNull(result.get(0).path());
    }

    private JsonSchemaValidator buildValidator(String schemaJson) {
        try {
            com.fasterxml.jackson.databind.JsonNode schemaNode = MAPPER.readTree(schemaJson);
            JsonSchema schema = JsonSchemaValidator.buildSchema(schemaNode, SpecVersion.VersionFlag.V202012);
            return new JsonSchemaValidator(schema);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
