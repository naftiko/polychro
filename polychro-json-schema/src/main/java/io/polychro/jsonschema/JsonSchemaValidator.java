package io.polychro.jsonschema;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import io.polychro.spi.Diagnostic;
import io.polychro.spi.Document;
import io.polychro.spi.Severity;
import io.polychro.spi.Validator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * JSON Schema validator that wraps networknt/json-schema-validator and produces unified diagnostics.
 */
class JsonSchemaValidator implements Validator {

    static final String NAME = "json-schema";

    private final JsonSchema schema;

    JsonSchemaValidator(JsonSchema schema) {
        this.schema = schema;
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

        Set<ValidationMessage> messages = schema.validate(doc.root());
        if (messages.isEmpty()) {
            return List.of();
        }

        List<Diagnostic> diagnostics = new ArrayList<>(messages.size());
        for (ValidationMessage msg : messages) {
            diagnostics.add(toDiagnostic(msg));
        }
        Collections.sort(diagnostics);
        return diagnostics;
    }

    Diagnostic toDiagnostic(ValidationMessage msg) {
        String path = msg.getPath();
        String code = msg.getType();
        String message = msg.getMessage();
        return new Diagnostic(Severity.ERROR, code, message, path, null);
    }

    static SpecVersion.VersionFlag detectDraft(JsonNode root) {
        if (root == null || !root.has("$schema")) {
            return SpecVersion.VersionFlag.V202012;
        }
        String schemaUri = root.get("$schema").asText("");
        if (schemaUri.contains("draft-04") || schemaUri.contains("draft/4")) {
            return SpecVersion.VersionFlag.V4;
        }
        if (schemaUri.contains("draft-06") || schemaUri.contains("draft/6")) {
            return SpecVersion.VersionFlag.V6;
        }
        if (schemaUri.contains("draft-07") || schemaUri.contains("draft/7")) {
            return SpecVersion.VersionFlag.V7;
        }
        if (schemaUri.contains("draft/2019-09") || schemaUri.contains("draft-2019-09")) {
            return SpecVersion.VersionFlag.V201909;
        }
        if (schemaUri.contains("draft/2020-12") || schemaUri.contains("draft-2020-12")) {
            return SpecVersion.VersionFlag.V202012;
        }
        return SpecVersion.VersionFlag.V202012;
    }

    static JsonSchema buildSchema(JsonNode schemaNode, SpecVersion.VersionFlag version) {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(version);
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        return factory.getSchema(schemaNode, config);
    }
}
