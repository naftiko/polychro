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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.polychro.spi.Diagnostic;
import io.polychro.spi.Document;
import io.polychro.spi.ValidatorConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonStructureInstanceValidationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static JsonNode personSchema;

    @BeforeAll
    static void loadSchema() throws IOException {
        try (InputStream is = JsonStructureInstanceValidationTest.class
                .getClassLoader().getResourceAsStream("schemas/person-schema.json")) {
            personSchema = MAPPER.readTree(is);
        }
    }

    private JsonStructureValidator instanceValidator() {
        return (JsonStructureValidator) new JsonStructureValidatorFactory()
                .create(new ValidatorConfig(Map.of("schemaNode", personSchema)));
    }

    @Test
    void validateShouldPassForValidInstance() {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("firstName", "Alice");
        root.put("lastName", "Smith");
        root.put("age", 30);
        Document doc = new Document(root, "test.json");

        List<Diagnostic> result = instanceValidator().validate(doc);
        assertTrue(result.isEmpty(), "Expected no errors, got: " + result);
    }

    @Test
    void validateShouldReportMissingRequiredField() {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("firstName", "Alice");
        // missing lastName
        Document doc = new Document(root, "test.json");

        List<Diagnostic> result = instanceValidator().validate(doc);
        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(d ->
                d.message() != null && d.message().toLowerCase().contains("required")));
    }

    @Test
    void validateShouldReportTypeMismatch() {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("firstName", "Alice");
        root.put("lastName", "Smith");
        root.put("age", "not-a-number");
        Document doc = new Document(root, "test.json");

        List<Diagnostic> result = instanceValidator().validate(doc);
        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(d ->
                d.path() != null && d.path().contains("age")));
    }

    @Test
    void validateShouldReportStringTypeMismatch() {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("firstName", 123);
        root.put("lastName", "Smith");
        Document doc = new Document(root, "test.json");

        List<Diagnostic> result = instanceValidator().validate(doc);
        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(d ->
                d.path() != null && d.path().contains("firstName")));
    }
}
