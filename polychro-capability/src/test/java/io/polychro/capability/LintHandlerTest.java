package io.polychro.capability;

import com.fasterxml.jackson.databind.JsonNode;
import io.naftiko.engine.step.StepHandlerContext;
import io.polychro.core.Linter;
import io.polychro.core.LinterConfig;
import io.polychro.spi.Diagnostic;
import io.polychro.spi.Severity;
import io.polychro.spi.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LintHandlerTest {

    @Test
    void executeShouldReturnDiagnosticsFromLinter() {
        Validator stubValidator = new Validator() {
            @Override
            public String name() {
                return "stub";
            }

            @Override
            public List<Diagnostic> validate(io.polychro.spi.Document doc) {
                return List.of(
                        new Diagnostic(Severity.ERROR, "rule-1", "Something is wrong", "$.info", null),
                        new Diagnostic(Severity.WARN, "rule-2", "Consider fixing", "$.name", null)
                );
            }
        };

        Linter linter = Linter.builder()
                .config(new LinterConfig(List.of(), Map.of(), false, "json-schema"))
                .addValidator(stubValidator)
                .build();

        LintHandler handler = new LintHandler(linter);

        StepHandlerContext context = new TestStepHandlerContext(
                Map.of("document", "{\"info\": \"test\"}", "format", "json"),
                Map.of(), Map.of()
        );

        JsonNode result = handler.execute(context);

        assertNotNull(result);
        assertEquals(2, result.get("count").asInt());
        assertTrue(result.get("has-errors").asBoolean());
        assertEquals(2, result.get("diagnostics").size());
    }

    @Test
    void executeShouldReturnEmptyWhenNoDiagnostics() {
        Validator noopValidator = new Validator() {
            @Override
            public String name() {
                return "noop";
            }

            @Override
            public List<Diagnostic> validate(io.polychro.spi.Document doc) {
                return List.of();
            }
        };

        Linter linter = Linter.builder()
                .config(new LinterConfig(List.of(), Map.of(), false, "json-schema"))
                .addValidator(noopValidator)
                .build();

        LintHandler handler = new LintHandler(linter);

        StepHandlerContext context = new TestStepHandlerContext(
                Map.of("document", "name: test", "format", "yaml"),
                Map.of(), Map.of()
        );

        JsonNode result = handler.execute(context);

        assertEquals(0, result.get("count").asInt());
        assertFalse(result.get("has-errors").asBoolean());
        assertTrue(result.get("diagnostics").isEmpty());
    }

    @Test
    void executeShouldAutoDetectFormatWhenNull() {
        Validator noop = new Validator() {
            @Override
            public String name() {
                return "noop";
            }

            @Override
            public List<Diagnostic> validate(io.polychro.spi.Document doc) {
                return List.of();
            }
        };

        Linter linter = Linter.builder()
                .config(new LinterConfig(List.of(), Map.of(), false, "json-schema"))
                .addValidator(noop)
                .build();

        LintHandler handler = new LintHandler(linter);

        // format is null, source-path is null
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("document", "{\"key\": \"value\"}");
        params.put("format", null);
        params.put("source-path", null);

        StepHandlerContext context = new TestStepHandlerContext(params, Map.of(), Map.of());

        JsonNode result = handler.execute(context);
        assertEquals(0, result.get("count").asInt());
    }

    @Test
    void buildResultShouldOmitNullCodeAndPath() {
        List<Diagnostic> diagnostics = List.of(
                new Diagnostic(Severity.INFO, null, "Info message", null, null)
        );

        JsonNode result = LintHandler.buildResult(diagnostics);

        JsonNode first = result.get("diagnostics").get(0);
        assertFalse(first.has("code"));
        assertFalse(first.has("path"));
        assertEquals("INFO", first.get("severity").asText());
        assertEquals("Info message", first.get("message").asText());
    }

    @Test
    void buildResultShouldIncludeCodeAndPathWhenPresent() {
        List<Diagnostic> diagnostics = List.of(
                new Diagnostic(Severity.ERROR, "my-rule", "Broken", "$.root", null)
        );

        JsonNode result = LintHandler.buildResult(diagnostics);

        JsonNode first = result.get("diagnostics").get(0);
        assertEquals("my-rule", first.get("code").asText());
        assertEquals("$.root", first.get("path").asText());
    }
}
