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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.polychro.spi.Diagnostic;
import io.polychro.spi.Severity;
import io.polychro.spi.SourceRange;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SarifFormatterTest {

    private final SarifFormatter formatter = new SarifFormatter();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void formatShouldProduceSarifStructure() throws Exception {
        Diagnostic d = new Diagnostic(Severity.ERROR, "rule1", "msg", "$.root", null);
        String result = formatter.format(List.of(d));
        JsonNode sarif = mapper.readTree(result);

        assertEquals("2.1.0", sarif.get("version").asText());
        assertTrue(sarif.has("$schema"));
        assertTrue(sarif.has("runs"));
        assertEquals(1, sarif.get("runs").size());
    }

    @Test
    void formatShouldIncludeToolInfo() throws Exception {
        Diagnostic d = new Diagnostic(Severity.WARN, null, "msg", null, null);
        String result = formatter.format(List.of(d));
        JsonNode sarif = mapper.readTree(result);
        JsonNode tool = sarif.get("runs").get(0).get("tool");
        JsonNode driver = tool.get("driver");
        assertEquals("polychro", driver.get("name").asText());
        assertEquals("0.1.0", driver.get("version").asText());
    }

    @Test
    void formatShouldMapErrorToErrorLevel() throws Exception {
        Diagnostic d = new Diagnostic(Severity.ERROR, "r1", "msg", null, null);
        String result = formatter.format(List.of(d));
        JsonNode sarif = mapper.readTree(result);
        JsonNode results = sarif.get("runs").get(0).get("results");
        assertEquals("error", results.get(0).get("level").asText());
    }

    @Test
    void formatShouldMapWarnToWarningLevel() throws Exception {
        Diagnostic d = new Diagnostic(Severity.WARN, "r1", "msg", null, null);
        String result = formatter.format(List.of(d));
        JsonNode sarif = mapper.readTree(result);
        JsonNode results = sarif.get("runs").get(0).get("results");
        assertEquals("warning", results.get(0).get("level").asText());
    }

    @Test
    void formatShouldMapInfoToNoteLevel() throws Exception {
        Diagnostic d = new Diagnostic(Severity.INFO, "r1", "msg", null, null);
        String result = formatter.format(List.of(d));
        JsonNode sarif = mapper.readTree(result);
        JsonNode results = sarif.get("runs").get(0).get("results");
        assertEquals("note", results.get(0).get("level").asText());
    }

    @Test
    void formatShouldMapHintToNoteLevel() throws Exception {
        Diagnostic d = new Diagnostic(Severity.HINT, "r1", "msg", null, null);
        String result = formatter.format(List.of(d));
        JsonNode sarif = mapper.readTree(result);
        JsonNode results = sarif.get("runs").get(0).get("results");
        assertEquals("note", results.get(0).get("level").asText());
    }

    @Test
    void formatShouldIncludeLocationWithPath() throws Exception {
        Diagnostic d = new Diagnostic(Severity.ERROR, "r1", "msg", "$.field", null);
        String result = formatter.format(List.of(d));
        JsonNode sarif = mapper.readTree(result);
        JsonNode location = sarif.get("runs").get(0).get("results").get(0).get("locations").get(0);
        assertEquals("$.field", location.get("physicalLocation").get("artifactLocation").get("uri").asText());
    }

    @Test
    void formatShouldIncludeLocationWithRange() throws Exception {
        SourceRange range = new SourceRange(10, 5, 12, 20);
        Diagnostic d = new Diagnostic(Severity.ERROR, "r1", "msg", null, range);
        String result = formatter.format(List.of(d));
        JsonNode sarif = mapper.readTree(result);
        JsonNode region = sarif.get("runs").get(0).get("results").get(0)
                .get("locations").get(0).get("physicalLocation").get("region");
        assertEquals(10, region.get("startLine").asInt());
        assertEquals(5, region.get("startColumn").asInt());
        assertEquals(12, region.get("endLine").asInt());
        assertEquals(20, region.get("endColumn").asInt());
    }

    @Test
    void formatShouldOmitLocationsWhenNoPathOrRange() throws Exception {
        Diagnostic d = new Diagnostic(Severity.WARN, "r1", "msg", null, null);
        String result = formatter.format(List.of(d));
        JsonNode sarif = mapper.readTree(result);
        JsonNode resultNode = sarif.get("runs").get(0).get("results").get(0);
        assertFalse(resultNode.has("locations"));
    }

    @Test
    void formatShouldHandleNullDiagnostics() throws Exception {
        String result = formatter.format(null);
        JsonNode sarif = mapper.readTree(result);
        assertEquals(0, sarif.get("runs").get(0).get("results").size());
    }

    @Test
    void formatShouldUseCodeAsRuleId() throws Exception {
        Diagnostic d = new Diagnostic(Severity.ERROR, "my-rule", "msg", null, null);
        String result = formatter.format(List.of(d));
        JsonNode sarif = mapper.readTree(result);
        JsonNode resultNode = sarif.get("runs").get(0).get("results").get(0);
        assertEquals("my-rule", resultNode.get("ruleId").asText());
    }

    @Test
    void formatShouldUseFallbackRuleIdWhenCodeIsNull() throws Exception {
        Diagnostic d = new Diagnostic(Severity.ERROR, null, "msg", null, null);
        String result = formatter.format(List.of(d));
        JsonNode sarif = mapper.readTree(result);
        JsonNode resultNode = sarif.get("runs").get(0).get("results").get(0);
        assertEquals("rule-0", resultNode.get("ruleId").asText());
    }

    @Test
    void formatShouldIncludeRuleDefinitions() throws Exception {
        Diagnostic d = new Diagnostic(Severity.ERROR, "test-rule", "msg", null, null);
        String result = formatter.format(List.of(d));
        JsonNode sarif = mapper.readTree(result);
        JsonNode rules = sarif.get("runs").get(0).get("tool").get("driver").get("rules");
        assertEquals(1, rules.size());
        assertEquals("test-rule", rules.get(0).get("id").asText());
    }

    @Test
    void formatShouldOmitShortDescriptionWhenCodeIsNull() throws Exception {
        Diagnostic d = new Diagnostic(Severity.ERROR, null, "msg", null, null);
        String result = formatter.format(List.of(d));
        JsonNode sarif = mapper.readTree(result);
        JsonNode rules = sarif.get("runs").get(0).get("tool").get("driver").get("rules");
        assertFalse(rules.get(0).has("shortDescription"));
    }
}
