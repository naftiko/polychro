package io.polychro.jsonschema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.SpecVersion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonSchemaAutoDetectionTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void detectDraftShouldReturn202012ForDraft202012Schema() {
        ObjectNode schema = MAPPER.createObjectNode();
        schema.put("$schema", "https://json-schema.org/draft/2020-12/schema");

        assertEquals(SpecVersion.VersionFlag.V202012, JsonSchemaValidator.detectDraft(schema));
    }

    @Test
    void detectDraftShouldReturnV7ForDraft07Schema() {
        ObjectNode schema = MAPPER.createObjectNode();
        schema.put("$schema", "http://json-schema.org/draft-07/schema#");

        assertEquals(SpecVersion.VersionFlag.V7, JsonSchemaValidator.detectDraft(schema));
    }

    @Test
    void detectDraftShouldReturnV4ForDraft04Schema() {
        ObjectNode schema = MAPPER.createObjectNode();
        schema.put("$schema", "http://json-schema.org/draft-04/schema#");

        assertEquals(SpecVersion.VersionFlag.V4, JsonSchemaValidator.detectDraft(schema));
    }

    @Test
    void detectDraftShouldReturnV6ForDraft06Schema() {
        ObjectNode schema = MAPPER.createObjectNode();
        schema.put("$schema", "http://json-schema.org/draft-06/schema#");

        assertEquals(SpecVersion.VersionFlag.V6, JsonSchemaValidator.detectDraft(schema));
    }

    @Test
    void detectDraftShouldReturnV201909ForDraft201909Schema() {
        ObjectNode schema = MAPPER.createObjectNode();
        schema.put("$schema", "https://json-schema.org/draft/2019-09/schema");

        assertEquals(SpecVersion.VersionFlag.V201909, JsonSchemaValidator.detectDraft(schema));
    }

    @Test
    void detectDraftShouldDefaultTo202012WhenNoSchemaField() {
        ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");

        assertEquals(SpecVersion.VersionFlag.V202012, JsonSchemaValidator.detectDraft(schema));
    }

    @Test
    void detectDraftShouldDefaultTo202012WhenNullRoot() {
        assertEquals(SpecVersion.VersionFlag.V202012, JsonSchemaValidator.detectDraft(null));
    }

    @Test
    void detectDraftShouldDefaultTo202012ForUnknownSchemaUri() {
        ObjectNode schema = MAPPER.createObjectNode();
        schema.put("$schema", "https://example.com/my-custom-schema");

        assertEquals(SpecVersion.VersionFlag.V202012, JsonSchemaValidator.detectDraft(schema));
    }

    // --- Alternate URI forms (second branch of || conditions) ---

    @Test
    void detectDraftShouldReturnV4ForSlashDraft4Uri() {
        ObjectNode schema = MAPPER.createObjectNode();
        schema.put("$schema", "https://json-schema.org/draft/4/schema");

        assertEquals(SpecVersion.VersionFlag.V4, JsonSchemaValidator.detectDraft(schema));
    }

    @Test
    void detectDraftShouldReturnV6ForSlashDraft6Uri() {
        ObjectNode schema = MAPPER.createObjectNode();
        schema.put("$schema", "https://json-schema.org/draft/6/schema");

        assertEquals(SpecVersion.VersionFlag.V6, JsonSchemaValidator.detectDraft(schema));
    }

    @Test
    void detectDraftShouldReturnV7ForSlashDraft7Uri() {
        ObjectNode schema = MAPPER.createObjectNode();
        schema.put("$schema", "https://json-schema.org/draft/7/schema");

        assertEquals(SpecVersion.VersionFlag.V7, JsonSchemaValidator.detectDraft(schema));
    }

    @Test
    void detectDraftShouldReturnV201909ForHyphenated201909Uri() {
        ObjectNode schema = MAPPER.createObjectNode();
        schema.put("$schema", "https://json-schema.org/draft-2019-09/schema");

        assertEquals(SpecVersion.VersionFlag.V201909, JsonSchemaValidator.detectDraft(schema));
    }

    @Test
    void detectDraftShouldReturnV202012ForHyphenated202012Uri() {
        ObjectNode schema = MAPPER.createObjectNode();
        schema.put("$schema", "https://json-schema.org/draft-2020-12/schema");

        assertEquals(SpecVersion.VersionFlag.V202012, JsonSchemaValidator.detectDraft(schema));
    }
}
