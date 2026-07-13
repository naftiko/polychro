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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.SpecVersion;
import io.polychro.spi.Diagnostic;
import io.polychro.spi.Document;
import io.polychro.spi.Severity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonSchemaValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static JsonSchema personSchema;

    @BeforeAll
    static void loadSchema() throws IOException {
        try (InputStream is = JsonSchemaValidatorTest.class.getResourceAsStream("/schemas/person-schema.json")) {
            JsonNode schemaNode = MAPPER.readTree(is);
            SpecVersion.VersionFlag version = JsonSchemaValidator.detectDraft(schemaNode);
            personSchema = JsonSchemaValidator.buildSchema(schemaNode, version);
        }
    }

    private JsonSchemaValidator validator() {
        return new JsonSchemaValidator(personSchema);
    }

    @Test
    void nameShouldReturnJsonSchema() {
        assertEquals("json-schema", validator().name());
    }

    // --- Valid document ---

    @Test
    void validateShouldReturnEmptyWhenDocumentIsValid() {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("name", "Alice");
        root.put("age", 30);
        Document doc = new Document(root, "test.json");

        List<Diagnostic> result = validator().validate(doc);

        assertTrue(result.isEmpty());
    }

    // --- Null input ---

    @Test
    void validateShouldReturnErrorWhenDocumentIsNull() {
        List<Diagnostic> result = validator().validate(null);

        assertEquals(1, result.size());
        assertEquals(Severity.ERROR, result.get(0).severity());
        assertEquals("null-input", result.get(0).code());
    }

    @Test
    void validateShouldReturnErrorWhenRootIsNull() {
        Document doc = new Document(null, "test.json");

        List<Diagnostic> result = validator().validate(doc);

        assertEquals(1, result.size());
        assertEquals("null-input", result.get(0).code());
    }

    // --- Type mismatch ---

    @Test
    void validateShouldReportTypeMismatch() {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("name", "Alice");
        root.put("age", "not-a-number");
        Document doc = new Document(root, "test.json");

        List<Diagnostic> result = validator().validate(doc);

        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(d -> d.path() != null && d.path().contains("age")));
    }

    // --- Missing required field ---

    @Test
    void validateShouldReportMissingRequiredField() {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("age", 25);
        Document doc = new Document(root, "test.json");

        List<Diagnostic> result = validator().validate(doc);

        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(d -> d.message().contains("name")));
    }

    // --- Pattern violation ---

    @Test
    void validateShouldReportPatternViolation() {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("name", "Bob");
        root.put("age", 20);
        root.put("email", "not-an-email");
        Document doc = new Document(root, "test.json");

        List<Diagnostic> result = validator().validate(doc);

        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(d -> d.path() != null && d.path().contains("email")));
    }

    // --- additionalProperties violation ---

    @Test
    void validateShouldReportAdditionalProperties() {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("name", "Charlie");
        root.put("age", 40);
        root.put("unknown", "value");
        Document doc = new Document(root, "test.json");

        List<Diagnostic> result = validator().validate(doc);

        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(d -> d.message().contains("unknown")
                || (d.path() != null && d.path().contains("unknown"))));
    }

    // --- Enum mismatch ---

    @Test
    void validateShouldReportEnumMismatch() {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("name", "Dave");
        root.put("age", 35);
        root.put("role", "superuser");
        Document doc = new Document(root, "test.json");

        List<Diagnostic> result = validator().validate(doc);

        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(d -> d.path() != null && d.path().contains("role")));
    }

    // --- minLength / maxLength ---

    @Test
    void validateShouldReportMinLengthViolation() {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("name", "");
        root.put("age", 10);
        Document doc = new Document(root, "test.json");

        List<Diagnostic> result = validator().validate(doc);

        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(d -> d.path() != null && d.path().contains("name")));
    }

    @Test
    void validateShouldReportMaxLengthViolation() {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("name", "A".repeat(51));
        root.put("age", 10);
        Document doc = new Document(root, "test.json");

        List<Diagnostic> result = validator().validate(doc);

        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(d -> d.path() != null && d.path().contains("name")));
    }

    // --- Nested object errors ---

    @Test
    void validateShouldReportNestedObjectErrors() {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("name", "Eve");
        root.put("age", 28);
        ObjectNode address = MAPPER.createObjectNode();
        address.put("street", "Main St");
        // missing "city"
        root.set("address", address);
        Document doc = new Document(root, "test.json");

        List<Diagnostic> result = validator().validate(doc);

        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(d -> d.path() != null && d.path().contains("address")));
    }

    // --- Array item errors ---

    @Test
    void validateShouldReportArrayItemErrors() {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("name", "Frank");
        root.put("age", 22);
        root.set("tags", MAPPER.createArrayNode().add("valid").add(123));
        Document doc = new Document(root, "test.json");

        List<Diagnostic> result = validator().validate(doc);

        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(d -> d.path() != null && d.path().contains("tags")));
    }

    // --- Multiple errors in one document ---

    @Test
    void validateShouldReportMultipleErrors() {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("age", "wrong-type");
        root.put("email", "bad");
        root.put("unknown", true);
        Document doc = new Document(root, "test.json");

        List<Diagnostic> result = validator().validate(doc);

        assertTrue(result.size() > 1);
    }

    // --- Diagnostics are sorted ---

    @Test
    void validateShouldReturnSortedDiagnostics() {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("age", "wrong");
        root.put("unknown", "x");
        Document doc = new Document(root, "test.json");

        List<Diagnostic> result = validator().validate(doc);

        for (int i = 0; i < result.size() - 1; i++) {
            assertTrue(result.get(i).compareTo(result.get(i + 1)) <= 0);
        }
    }

    // --- $ref resolution (nested schema) ---

    @Test
    void validateShouldResolveRefInSchema() throws IOException {
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "$defs": {
                    "name": { "type": "string", "minLength": 1 }
                  },
                  "type": "object",
                  "properties": {
                    "firstName": { "$ref": "#/$defs/name" }
                  },
                  "required": ["firstName"]
                }
                """;
        JsonNode schemaNode = MAPPER.readTree(schemaJson);
        JsonSchema schema = JsonSchemaValidator.buildSchema(schemaNode, SpecVersion.VersionFlag.V202012);
        JsonSchemaValidator v = new JsonSchemaValidator(schema);

        ObjectNode root = MAPPER.createObjectNode();
        root.put("firstName", "");
        Document doc = new Document(root, "test.json");

        List<Diagnostic> result = v.validate(doc);

        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(d -> d.path() != null && d.path().contains("firstName")));
    }

    // --- toDiagnostic ---

    @Test
    void toDiagnosticShouldMapSeverityToError() {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("age", "bad");
        Document doc = new Document(root, "test.json");

        List<Diagnostic> result = validator().validate(doc);

        assertFalse(result.isEmpty());
        result.forEach(d -> assertEquals(Severity.ERROR, d.severity()));
    }
}
