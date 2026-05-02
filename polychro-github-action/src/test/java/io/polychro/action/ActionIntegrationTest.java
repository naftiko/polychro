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
package io.polychro.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.polychro.core.SarifFormatter;
import io.polychro.spi.Diagnostic;
import io.polychro.spi.Severity;
import io.polychro.spi.SourceRange;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ActionIntegrationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void sarifOutputShouldHaveValidStructure() throws Exception {
        List<Diagnostic> diagnostics = List.of(
                new Diagnostic(Severity.ERROR, "no-trailing-slash", "Remove trailing slash",
                        "$.consumes[0].baseUri", null));

        String sarif = new SarifFormatter().format(diagnostics);
        JsonNode root = MAPPER.readTree(sarif);

        assertEquals("2.1.0", root.get("version").asText());
        assertTrue(root.has("$schema"));
        assertTrue(root.has("runs"));
        assertEquals(1, root.get("runs").size());
    }

    @Test
    void sarifOutputShouldContainRuleMetadata() throws Exception {
        List<Diagnostic> diagnostics = List.of(
                new Diagnostic(Severity.WARN, "capability-tags-present", "Add tags",
                        "$.info", null));

        String sarif = new SarifFormatter().format(diagnostics);
        JsonNode root = MAPPER.readTree(sarif);
        JsonNode rules = root.get("runs").get(0).get("tool").get("driver").get("rules");

        assertEquals(1, rules.size());
        assertEquals("capability-tags-present", rules.get(0).get("id").asText());
    }

    @Test
    void sarifOutputShouldMapSeverityCorrectly() throws Exception {
        List<Diagnostic> diagnostics = List.of(
                new Diagnostic(Severity.ERROR, "e", "err", "$", null),
                new Diagnostic(Severity.WARN, "w", "warn", "$", null),
                new Diagnostic(Severity.INFO, "i", "info", "$", null));

        String sarif = new SarifFormatter().format(diagnostics);
        JsonNode root = MAPPER.readTree(sarif);
        JsonNode results = root.get("runs").get(0).get("results");

        assertEquals("error", results.get(0).get("level").asText());
        assertEquals("warning", results.get(1).get("level").asText());
        assertEquals("note", results.get(2).get("level").asText());
    }

    @Test
    void sarifOutputShouldMapLocationsWithRange() throws Exception {
        SourceRange range = new SourceRange(5, 2, 5, 20);
        List<Diagnostic> diagnostics = List.of(
                new Diagnostic(Severity.ERROR, "test", "msg", "file.yml", range));

        String sarif = new SarifFormatter().format(diagnostics);
        JsonNode root = MAPPER.readTree(sarif);
        JsonNode location = root.get("runs").get(0).get("results").get(0)
                .get("locations").get(0).get("physicalLocation");

        assertEquals("file.yml", location.get("artifactLocation").get("uri").asText());
        assertEquals(5, location.get("region").get("startLine").asInt());
        assertEquals(2, location.get("region").get("startColumn").asInt());
    }

    @Test
    void sarifOutputShouldHandleEmptyDiagnostics() throws Exception {
        String sarif = new SarifFormatter().format(List.of());
        JsonNode root = MAPPER.readTree(sarif);

        assertEquals("2.1.0", root.get("version").asText());
        assertEquals(0, root.get("runs").get(0).get("results").size());
        assertEquals(0, root.get("runs").get(0).get("tool").get("driver").get("rules").size());
    }

    @Test
    void sarifOutputShouldHandleDiagnosticsWithoutRange() throws Exception {
        List<Diagnostic> diagnostics = List.of(
                new Diagnostic(Severity.WARN, "rule", "msg", "$.info.name", null));

        String sarif = new SarifFormatter().format(diagnostics);
        JsonNode root = MAPPER.readTree(sarif);
        JsonNode location = root.get("runs").get(0).get("results").get(0)
                .get("locations").get(0).get("physicalLocation");

        assertTrue(location.has("artifactLocation"));
        assertFalse(location.has("region"));
    }

    @Test
    void exitCodeShouldBe0WhenClean() {
        int code = FailOnThreshold.computeExitCode(List.of(), "error");
        assertEquals(0, code);
    }

    @Test
    void exitCodeShouldBe1WhenThresholdExceeded() {
        List<Diagnostic> diagnostics = List.of(
                new Diagnostic(Severity.ERROR, "rule", "msg", "$", null));
        int code = FailOnThreshold.computeExitCode(diagnostics, "error");
        assertEquals(1, code);
    }

    @Test
    void cacheKeyShouldBeDeterministic() {
        String key1 = FailOnThreshold.cacheKey("0.1.0", "linux-x64");
        String key2 = FailOnThreshold.cacheKey("0.1.0", "linux-x64");
        assertEquals(key1, key2);
        assertEquals("polychro-0.1.0-linux-x64", key1);
    }
}
