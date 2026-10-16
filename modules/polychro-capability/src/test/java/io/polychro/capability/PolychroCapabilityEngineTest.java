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
package io.polychro.capability;

import io.ikanos.engine.exposes.ServerAdapter;
import io.ikanos.engine.exposes.mcp.McpServerAdapter;
import io.ikanos.spec.consumes.ClientSpec;
import io.ikanos.spec.util.IkanosMappers;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test: builds the capability from the embedded {@code polychro-capability.yml} with
 * the real Ikanos engine (telemetry off, the {@link PolychroCapability.Builder} default) and runs
 * a tool call through it.
 *
 * <p>Unlike the handler unit tests, this goes through spec loading, adapter construction, step
 * dispatch and output mapping, so a runtime classpath conflict between Polychro and the embedded
 * engine (Jackson 2 vs 3, Restlet 2 vs 3, json-schema-validator 1.x vs 3.x) fails the build
 * instead of only surfacing at {@code polychro serve} time.</p>
 */
class PolychroCapabilityEngineTest {

    @Test
    void lintToolCallShouldRunThroughTheEmbeddedEngine() throws Exception {
        PolychroCapability capability = PolychroCapability.builder().build();

        McpSchema.CallToolResult result = mcpAdapter(capability).getToolHandler()
                .handleToolCall("lint", Map.of("document", "openapi: 3.1.0\n", "format", "yaml"));

        assertFalse(Boolean.TRUE.equals(result.isError()), () -> "Tool call failed: " + result);
        JsonNode payload = IkanosMappers.json().readTree(
                assertInstanceOf(McpSchema.TextContent.class, result.content().get(0)).text());
        assertTrue(payload.has("count"), () -> "Missing 'count' in " + payload);
        assertTrue(payload.has("has-errors"), () -> "Missing 'has-errors' in " + payload);
    }

    @Test
    void embeddedEngineShouldRunOnRestlet3() {
        // The polychro-capability pom used to pin org.restlet:org.restlet:2.7.0-m2 next to the
        // Restlet 3 extensions brought by ikanos-engine. Only one Restlet line may be loaded.
        assertEquals("3", org.restlet.engine.Engine.MAJOR_NUMBER);
    }

    @Test
    void capabilityShouldNotConsumeMcpServers() {
        // Maven resolves com.networknt:json-schema-validator to Polychro's 1.x line, which lacks
        // the 3.x classes the Ikanos MCP client adapter needs (see the root pom). Consuming an MCP
        // server from polychro-capability.yml would need json-schema-validator 3.x first.
        PolychroCapability capability = PolychroCapability.builder().build();

        for (ClientSpec client : capability.getSpec().getCapability().getConsumes()) {
            assertFalse("mcp".equals(client.getType()),
                    "polychro-capability.yml consumes an MCP server: " + client.getNamespace());
        }
    }

    private static McpServerAdapter mcpAdapter(PolychroCapability capability) {
        for (ServerAdapter adapter : capability.getServerAdapters()) {
            if (adapter instanceof McpServerAdapter mcp) {
                return mcp;
            }
        }
        throw new AssertionError("polychro-capability.yml exposes no MCP server");
    }
}
