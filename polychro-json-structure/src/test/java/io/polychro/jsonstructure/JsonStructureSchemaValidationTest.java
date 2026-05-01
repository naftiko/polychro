package io.polychro.jsonstructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.polychro.spi.Diagnostic;
import io.polychro.spi.Document;
import io.polychro.spi.Severity;
import io.polychro.spi.ValidatorConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonStructureSchemaValidationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonStructureValidator schemaValidator() {
        return (JsonStructureValidator) new JsonStructureValidatorFactory()
                .create(new ValidatorConfig(Map.of("mode", "schema")));
    }

    @Test
    void validateShouldPassForValidSchema() throws IOException {
        Document doc = loadDocument("schemas/person-schema.json");
        List<Diagnostic> result = schemaValidator().validate(doc);
        assertTrue(result.isEmpty(), "Expected no errors for valid schema, got: " + result);
    }

    @Test
    void validateShouldReportMissingId() throws IOException {
        Document doc = loadDocument("schemas/missing-id-schema.json");
        List<Diagnostic> result = schemaValidator().validate(doc);
        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(d ->
                d.code() != null && d.code().contains("ROOT_MISSING")));
    }

    @Test
    void validateShouldReportInvalidType() throws IOException {
        Document doc = loadDocument("schemas/invalid-type-schema.json");
        List<Diagnostic> result = schemaValidator().validate(doc);
        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(d ->
                d.code() != null && d.code().contains("TYPE")));
    }

    @Test
    void validateShouldReportErrorForNullDocument() {
        List<Diagnostic> result = schemaValidator().validate(null);
        assertEquals(1, result.size());
        assertEquals(Severity.ERROR, result.get(0).severity());
        assertEquals("null-input", result.get(0).code());
    }

    @Test
    void validateShouldReportErrorForNullRoot() {
        Document doc = new Document(null, "test.json");
        List<Diagnostic> result = schemaValidator().validate(doc);
        assertEquals(1, result.size());
        assertEquals("null-input", result.get(0).code());
    }

    private Document loadDocument(String path) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(is, "Resource not found: " + path);
            JsonNode root = MAPPER.readTree(is);
            return new Document(root, path);
        }
    }
}
