package io.polychro.capability;

import com.fasterxml.jackson.databind.JsonNode;
import io.naftiko.engine.step.StepHandlerContext;
import io.polychro.core.Linter;
import io.polychro.core.LinterConfig;
import io.polychro.spi.Diagnostic;
import io.polychro.spi.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ListRulesHandlerTest {

    @Test
    void executeShouldReturnAllValidatorNames() {
        Validator v1 = new Validator() {
            @Override
            public String name() {
                return "wellformedness";
            }

            @Override
            public List<Diagnostic> validate(io.polychro.spi.Document doc) {
                return List.of();
            }
        };

        Validator v2 = new Validator() {
            @Override
            public String name() {
                return "json-schema";
            }

            @Override
            public List<Diagnostic> validate(io.polychro.spi.Document doc) {
                return List.of();
            }
        };

        Linter linter = Linter.builder()
                .config(new LinterConfig(List.of(), Map.of(), false, "json-schema"))
                .addValidator(v1)
                .addValidator(v2)
                .build();

        ListRulesHandler handler = new ListRulesHandler(linter);

        StepHandlerContext context = new TestStepHandlerContext(Map.of(), Map.of(), Map.of());

        JsonNode result = handler.execute(context);

        assertEquals(2, result.get("count").asInt());
        assertEquals(2, result.get("validators").size());
        assertEquals("wellformedness", result.get("validators").get(0).get("name").asText());
        assertEquals("json-schema", result.get("validators").get(1).get("name").asText());
    }

    @Test
    void executeShouldReturnEmptyWhenNoValidators() {
        Linter linter = Linter.builder()
                .config(new LinterConfig(List.of(), Map.of(), false, "json-schema"))
                .build();

        ListRulesHandler handler = new ListRulesHandler(linter);

        StepHandlerContext context = new TestStepHandlerContext(Map.of(), Map.of(), Map.of());

        JsonNode result = handler.execute(context);

        assertEquals(0, result.get("count").asInt());
        assertTrue(result.get("validators").isEmpty());
    }
}
