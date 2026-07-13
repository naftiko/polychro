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
