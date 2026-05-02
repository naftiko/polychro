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
import io.polychro.spi.Diagnostic;
import io.polychro.spi.Document;
import io.polychro.spi.Severity;
import io.polychro.spi.SourceRange;
import io.polychro.spi.Validator;
import org.json_structure.validation.InstanceValidator;
import org.json_structure.validation.JsonLocation;
import org.json_structure.validation.SchemaValidator;
import org.json_structure.validation.ValidationError;
import org.json_structure.validation.ValidationOptions;
import org.json_structure.validation.ValidationResult;
import org.json_structure.validation.ValidationSeverity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * JSON Structure validator that wraps the official SDK and produces unified diagnostics.
 * <p>
 * Supports two modes:
 * <ul>
 *   <li><b>Schema mode</b> — validates that a document is a well-formed JSON Structure schema</li>
 *   <li><b>Instance mode</b> — validates data against a JSON Structure schema</li>
 * </ul>
 */
class JsonStructureValidator implements Validator {

    static final String NAME = "json-structure";
    static final String JSON_STRUCTURE_SCHEMA_PREFIX = "https://json-structure.org/meta/";

    private final JsonNode schema;
    private final ValidationOptions options;
    private final Mode mode;

    enum Mode {
        SCHEMA,
        INSTANCE
    }

    JsonStructureValidator(JsonNode schema, ValidationOptions options, Mode mode) {
        this.schema = schema;
        this.options = options;
        this.mode = mode;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public List<Diagnostic> validate(Document doc) {
        if (doc == null || doc.root() == null) {
            return List.of(new Diagnostic(Severity.ERROR, "null-input",
                    "Document or root node is null", null, null));
        }

        ValidationResult result = executeValidation(doc.root());
        List<ValidationError> errors = result.getErrors();
        if (errors.isEmpty()) {
            return List.of();
        }

        List<Diagnostic> diagnostics = new ArrayList<>(errors.size());
        for (ValidationError error : errors) {
            diagnostics.add(toDiagnostic(error));
        }
        Collections.sort(diagnostics);
        return diagnostics;
    }

    ValidationResult executeValidation(JsonNode root) {
        if (mode == Mode.SCHEMA) {
            SchemaValidator schemaValidator = new SchemaValidator(options);
            return schemaValidator.validate(root);
        }
        InstanceValidator instanceValidator = new InstanceValidator(options);
        return instanceValidator.validate(root, schema);
    }

    Diagnostic toDiagnostic(ValidationError error) {
        Severity severity = mapSeverity(error.getSeverity());
        String path = error.getPath();
        String code = error.getCode();
        String message = error.getMessage();
        SourceRange range = mapLocation(error.getLocation());
        return new Diagnostic(severity, code, message, path, range);
    }

    static Severity mapSeverity(ValidationSeverity severity) {
        if (severity == null) {
            return Severity.ERROR;
        }
        return switch (severity) {
            case ERROR -> Severity.ERROR;
            case WARNING -> Severity.WARN;
        };
    }

    static SourceRange mapLocation(JsonLocation location) {
        if (location == null || !location.isKnown()) {
            return null;
        }
        return new SourceRange(location.getLine(), location.getColumn(),
                location.getLine(), location.getColumn());
    }

    static boolean isJsonStructureDocument(JsonNode root) {
        if (root == null || !root.has("$schema")) {
            return false;
        }
        String schemaUri = root.get("$schema").asText("");
        return schemaUri.startsWith(JSON_STRUCTURE_SCHEMA_PREFIX);
    }
}
