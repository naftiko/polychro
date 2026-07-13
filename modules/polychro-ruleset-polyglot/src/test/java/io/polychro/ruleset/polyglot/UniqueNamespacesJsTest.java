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
package io.polychro.ruleset.polyglot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.polychro.ruleset.RuleFunction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UniqueNamespacesJsTest {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
    private static RuleFunction function;

    @BeforeAll
    static void setUp() {
        Path functionsDir = Path.of("src/test/resources/functions").toAbsolutePath();
        PolyglotFunctionProvider provider = PolyglotFunctionProvider.forDirectory(
                functionsDir, List.of("unique-namespaces"));
        function = provider.functions().get(0);
    }

    @Test
    void shouldDetectDuplicateNamespace() throws Exception {
        String yaml = """
                capability:
                  consumes:
                    - namespace: my-api
                      type: http
                  exposes:
                    - namespace: my-api
                      type: rest
                """;
        JsonNode root = YAML.readTree(yaml);
        List<String> results = function.evaluate(root, Map.of());
        assertFalse(results.isEmpty());
        assertTrue(results.get(0).contains("my-api"));
        assertTrue(results.get(0).contains("already used"));
    }

    @Test
    void shouldPassWithUniqueNamespaces() throws Exception {
        String yaml = """
                capability:
                  consumes:
                    - namespace: github-api
                      type: http
                  exposes:
                    - namespace: my-rest
                      type: rest
                """;
        JsonNode root = YAML.readTree(yaml);
        List<String> results = function.evaluate(root, Map.of());
        assertTrue(results.isEmpty());
    }

    @Test
    void shouldHandleEmptyDocument() throws Exception {
        JsonNode root = YAML.readTree("{}");
        List<String> results = function.evaluate(root, Map.of());
        assertTrue(results.isEmpty());
    }
}
