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
