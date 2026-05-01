package io.polychro.core;

import com.fasterxml.jackson.databind.JsonNode;
import io.polychro.spi.Document;

/**
 * Detects whether a document uses JSON Schema or JSON Structure based on
 * the {@code $schema} field, and returns the appropriate validator name.
 */
class SchemaFormatRouter {

    private static final String JSON_STRUCTURE_INDICATOR = "json-structure";
    private static final String JSON_SCHEMA_VALIDATOR = "json-schema";
    private static final String JSON_STRUCTURE_VALIDATOR = "json-structure";

    private SchemaFormatRouter() {
    }

    /**
     * Determine which schema validator should be used for the given document.
     *
     * @param doc                    the document to inspect
     * @param defaultSchemaValidator the default validator when no $schema is present
     * @return the validator name to use ("json-schema" or "json-structure")
     */
    static String detectSchemaValidator(Document doc, String defaultSchemaValidator) {
        JsonNode root = doc.root();
        if (root == null || !root.isObject()) {
            return defaultSchemaValidator;
        }

        JsonNode schemaNode = root.get("$schema");
        if (schemaNode == null || !schemaNode.isTextual()) {
            return defaultSchemaValidator;
        }

        String schemaUri = schemaNode.asText();
        if (schemaUri.contains(JSON_STRUCTURE_INDICATOR)) {
            return JSON_STRUCTURE_VALIDATOR;
        }

        return JSON_SCHEMA_VALIDATOR;
    }
}
