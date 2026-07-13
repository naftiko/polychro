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
import io.ikanos.engine.step.StepHandlerContext;
import io.polychro.core.LinterConfig;
import io.polychro.spi.Diagnostic;
import io.polychro.spi.Severity;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValidateSchemaHandlerTest {

    @Test
    void executeShouldUseSchemaFromInput() {
        LinterConfig baseConfig = new LinterConfig(List.of(), Map.of(), false, "json-schema");
        ValidateSchemaHandler handler = new ValidateSchemaHandler(baseConfig);

        Map<String, Object> params = new HashMap<>();
        params.put("document", "{\"name\": \"test\"}");
        params.put("schema", "nonexistent-schema.json");
        params.put("format", "json");

        StepHandlerContext context = new TestStepHandlerContext(params, Map.of(), Map.of());

        // This should either produce schema diagnostics or empty (no json-schema validator on classpath)
        JsonNode result = handler.execute(context);
        assertNotNull(result);
        assertTrue(result.has("valid"));
        assertTrue(result.has("count"));
        assertTrue(result.has("diagnostics"));
    }

    @Test
    void executeShouldFallBackToBaseConfigWhenSchemaNull() {
        LinterConfig baseConfig = new LinterConfig(
                List.of("json-schema"),
                Map.of("json-schema", Map.of("schema", "some-schema.json")),
                false,
                "json-schema"
        );
        ValidateSchemaHandler handler = new ValidateSchemaHandler(baseConfig);

        Map<String, Object> params = new HashMap<>();
        params.put("document", "{\"x\": 1}");
        params.put("schema", null);
        params.put("format", "json");

        StepHandlerContext context = new TestStepHandlerContext(params, Map.of(), Map.of());

        JsonNode result = handler.execute(context);
        assertNotNull(result);
        assertTrue(result.has("valid"));
    }

    @Test
    void executeShouldFallBackToBaseConfigWhenSchemaBlank() {
        LinterConfig baseConfig = new LinterConfig(
                List.of(),
                Map.of("json-schema", Map.of("schema", "fallback.json")),
                false,
                "json-schema"
        );
        ValidateSchemaHandler handler = new ValidateSchemaHandler(baseConfig);

        Map<String, Object> params = new HashMap<>();
        params.put("document", "key: value");
        params.put("schema", "   ");
        params.put("format", "yaml");

        StepHandlerContext context = new TestStepHandlerContext(params, Map.of(), Map.of());

        JsonNode result = handler.execute(context);
        assertNotNull(result);
    }

    @Test
    void buildSchemaConfigShouldUseProvidedSchema() {
        LinterConfig baseConfig = LinterConfig.defaults();
        ValidateSchemaHandler handler = new ValidateSchemaHandler(baseConfig);

        LinterConfig result = handler.buildSchemaConfig("my-schema.json");

        assertEquals(List.of("schema-model"), result.validators());
        assertEquals("my-schema.json",
                result.validatorConfigs().get("json-schema").get("schemaPath"));
        assertEquals("my-schema.json",
                result.validatorConfigs().get("json-structure").get("schemaPath"));
    }

    @Test
    void buildSchemaConfigShouldFallBackWhenSchemaNull() {
        LinterConfig baseConfig = new LinterConfig(
                List.of(),
                Map.of(
                        "json-schema", Map.of("schemaPath", "base-schema.json"),
                        "json-structure", Map.of("schemaPath", "base-structure.json")
                ),
                false,
                "json-structure"
        );
        ValidateSchemaHandler handler = new ValidateSchemaHandler(baseConfig);

        LinterConfig result = handler.buildSchemaConfig(null);

        assertEquals("base-schema.json",
                result.validatorConfigs().get("json-schema").get("schemaPath"));
        assertEquals("base-structure.json",
                result.validatorConfigs().get("json-structure").get("schemaPath"));
        assertEquals("json-structure", result.defaultSchemaValidator());
    }

    @Test
    void buildSchemaConfigShouldUseEmptyMapWhenNoBaseConfig() {
        LinterConfig baseConfig = new LinterConfig(List.of(), Map.of(), false, "json-schema");
        ValidateSchemaHandler handler = new ValidateSchemaHandler(baseConfig);

        LinterConfig result = handler.buildSchemaConfig(null);

        assertTrue(result.validatorConfigs().get("json-schema").isEmpty());
        assertTrue(result.validatorConfigs().get("json-structure").isEmpty());
    }

    @Test
    void buildResultShouldShowValidTrueForEmptyDiagnostics() {
        JsonNode result = ValidateSchemaHandler.buildResult(List.of());

        assertTrue(result.get("valid").asBoolean());
        assertEquals(0, result.get("count").asInt());
        assertTrue(result.get("diagnostics").isEmpty());
    }

    @Test
    void buildResultShouldShowValidFalseForDiagnostics() {
        List<Diagnostic> diagnostics = List.of(
                new Diagnostic(Severity.ERROR, "schema", "Missing field", "$.name", null)
        );

        JsonNode result = ValidateSchemaHandler.buildResult(diagnostics);

        assertFalse(result.get("valid").asBoolean());
        assertEquals(1, result.get("count").asInt());
        assertEquals("ERROR", result.get("diagnostics").get(0).get("severity").asText());
    }

    @Test
    void buildResultShouldOmitNullCodeAndPath() {
        List<Diagnostic> diagnostics = List.of(
                new Diagnostic(Severity.WARN, null, "Something", null, null)
        );

        JsonNode result = ValidateSchemaHandler.buildResult(diagnostics);

        JsonNode first = result.get("diagnostics").get(0);
        assertFalse(first.has("code"));
        assertFalse(first.has("path"));
    }
}
