package io.polychro.ruleset;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Schema function — validates the target node against an inline JSON Schema.
 * <p>
 * Options:
 * <ul>
 *     <li>{@code schema} — the JSON Schema object to validate against</li>
 *     <li>{@code dialect} — optional schema dialect (default: draft-07)</li>
 * </ul>
 */
class SchemaFunction implements RuleFunction {

    private static final JsonSchemaFactory FACTORY = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = new com.fasterxml.jackson.databind.ObjectMapper();

    @Override
    public String name() {
        return "schema";
    }

    @Override
    public List<String> evaluate(JsonNode targetNode, Map<String, Object> options) {
        if (targetNode == null || targetNode.isNull() || targetNode.isMissingNode()) {
            return List.of("Value must not be null for schema validation");
        }

        Object schemaObj = options.get("schema");
        if (schemaObj == null) {
            return List.of("Schema function requires a 'schema' option");
        }

        JsonNode schemaNode;
        if (schemaObj instanceof Map<?, ?>) {
            schemaNode = MAPPER.valueToTree(schemaObj);
        } else {
            return List.of("Schema function 'schema' option must be an object");
        }

        JsonSchema schema = FACTORY.getSchema(schemaNode, new SchemaValidatorsConfig());
        Set<ValidationMessage> errors = schema.validate(targetNode);

        if (errors.isEmpty()) {
            return List.of();
        }

        return errors.stream()
                .map(ValidationMessage::getMessage)
                .collect(Collectors.toList());
    }
}
