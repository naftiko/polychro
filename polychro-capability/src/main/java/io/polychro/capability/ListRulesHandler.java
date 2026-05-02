package io.polychro.capability;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.naftiko.engine.step.StepHandler;
import io.naftiko.engine.step.StepHandlerContext;
import io.polychro.core.Linter;
import io.polychro.spi.Validator;

import java.util.List;

/**
 * Step handler for the "do-list-rules" step. Returns metadata about all active validators.
 */
class ListRulesHandler implements StepHandler {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Linter linter;

    ListRulesHandler(Linter linter) {
        this.linter = linter;
    }

    @Override
    public JsonNode execute(StepHandlerContext context) {
        List<Validator> validators = linter.validators();

        ObjectNode result = MAPPER.createObjectNode();
        ArrayNode array = MAPPER.createArrayNode();

        for (Validator v : validators) {
            ObjectNode entry = MAPPER.createObjectNode();
            entry.put("name", v.name());
            array.add(entry);
        }

        result.set("validators", array);
        result.put("count", validators.size());

        return result;
    }
}
