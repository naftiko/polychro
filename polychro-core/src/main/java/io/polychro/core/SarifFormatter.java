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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.polychro.spi.Diagnostic;

import java.util.List;

/**
 * Formats diagnostics as SARIF 2.1.0 (Static Analysis Results Interchange Format).
 */
public class SarifFormatter implements DiagnosticFormatter {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String SARIF_VERSION = "2.1.0";
    private static final String SARIF_SCHEMA = "https://docs.oasis-open.org/sarif/sarif/v2.1.0/sarif-schema-2.1.0.json";

    @Override
    public String format(List<Diagnostic> diagnostics) {
        ObjectNode sarif = MAPPER.createObjectNode();
        sarif.put("version", SARIF_VERSION);
        sarif.put("$schema", SARIF_SCHEMA);

        ArrayNode runs = MAPPER.createArrayNode();
        ObjectNode run = MAPPER.createObjectNode();

        // Tool info
        ObjectNode tool = MAPPER.createObjectNode();
        ObjectNode driver = MAPPER.createObjectNode();
        driver.put("name", "polychro");
        driver.put("version", "0.1.0");

        // Rules
        ArrayNode rules = MAPPER.createArrayNode();
        ArrayNode results = MAPPER.createArrayNode();

        if (diagnostics != null) {
            for (int i = 0; i < diagnostics.size(); i++) {
                Diagnostic d = diagnostics.get(i);
                String ruleId = d.code() != null ? d.code() : "rule-" + i;

                // Add rule definition
                ObjectNode ruleNode = MAPPER.createObjectNode();
                ruleNode.put("id", ruleId);
                if (d.code() != null) {
                    ObjectNode shortDesc = MAPPER.createObjectNode();
                    shortDesc.put("text", d.code());
                    ruleNode.set("shortDescription", shortDesc);
                }
                rules.add(ruleNode);

                // Add result
                ObjectNode result = MAPPER.createObjectNode();
                result.put("ruleId", ruleId);
                result.put("level", sarifLevel(d));
                ObjectNode message = MAPPER.createObjectNode();
                message.put("text", d.message());
                result.set("message", message);

                if (d.path() != null || d.range() != null) {
                    ArrayNode locations = MAPPER.createArrayNode();
                    ObjectNode location = MAPPER.createObjectNode();
                    ObjectNode physicalLocation = MAPPER.createObjectNode();

                    if (d.path() != null) {
                        ObjectNode artifactLocation = MAPPER.createObjectNode();
                        artifactLocation.put("uri", d.path());
                        physicalLocation.set("artifactLocation", artifactLocation);
                    }
                    if (d.range() != null) {
                        ObjectNode region = MAPPER.createObjectNode();
                        region.put("startLine", d.range().startLine());
                        region.put("startColumn", d.range().startColumn());
                        region.put("endLine", d.range().endLine());
                        region.put("endColumn", d.range().endColumn());
                        physicalLocation.set("region", region);
                    }

                    location.set("physicalLocation", physicalLocation);
                    locations.add(location);
                    result.set("locations", locations);
                }

                results.add(result);
            }
        }

        driver.set("rules", rules);
        tool.set("driver", driver);
        run.set("tool", tool);
        run.set("results", results);
        runs.add(run);
        sarif.set("runs", runs);

        return sarif.toPrettyString();
    }

    private String sarifLevel(Diagnostic d) {
        return switch (d.severity()) {
            case ERROR -> "error";
            case WARN -> "warning";
            case INFO -> "note";
            case HINT -> "note";
        };
    }
}
