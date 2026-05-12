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
 */package io.polychro.capability;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.ikanos.engine.step.StepHandler;
import io.ikanos.engine.step.StepHandlerContext;
import io.polychro.core.Linter;
import io.polychro.core.LinterConfig;
import io.polychro.spi.Diagnostic;
import io.polychro.spi.Document;

import java.util.List;
import java.util.Map;

/**
 * Step handler for the "do-validate-schema" step. Runs only the JSON Schema validator
 * on a document (fast path — no rules, no wellformedness).
 */
class ValidateSchemaHandler implements StepHandler {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final LinterConfig baseConfig;

    ValidateSchemaHandler(LinterConfig baseConfig) {
        this.baseConfig = baseConfig;
    }

    @Override
    public JsonNode execute(StepHandlerContext context) {
        String content = (String) context.inputParameter("document");
        String schema = (String) context.inputParameter("schema");
        String format = (String) context.inputParameter("format");

        Document doc = Document.fromString(content, format, null);

        LinterConfig schemaConfig = buildSchemaConfig(schema);
        Linter schemaLinter = Linter.builder().config(schemaConfig).build();
        List<Diagnostic> diagnostics = schemaLinter.lint(doc);

        return buildResult(diagnostics);
    }

    LinterConfig buildSchemaConfig(String schema) {
        Map<String, Object> schemaProps;
        if (schema != null && !schema.isBlank()) {
            schemaProps = Map.of("schema", schema);
        } else {
            Map<String, Map<String, Object>> base = baseConfig.validatorConfigs();
            schemaProps = base.getOrDefault("json-schema", Map.of());
        }

        return new LinterConfig(
                List.of("json-schema"),
                Map.of("json-schema", schemaProps),
                false,
                "json-schema"
        );
    }

    static JsonNode buildResult(List<Diagnostic> diagnostics) {
        ObjectNode result = MAPPER.createObjectNode();
        ArrayNode array = MAPPER.createArrayNode();

        for (Diagnostic d : diagnostics) {
            ObjectNode entry = MAPPER.createObjectNode();
            entry.put("severity", d.severity().name());
            if (d.code() != null) {
                entry.put("code", d.code());
            }
            entry.put("message", d.message());
            if (d.path() != null) {
                entry.put("path", d.path());
            }
            array.add(entry);
        }

        result.set("diagnostics", array);
        result.put("count", diagnostics.size());
        result.put("valid", diagnostics.isEmpty());

        return result;
    }
}
