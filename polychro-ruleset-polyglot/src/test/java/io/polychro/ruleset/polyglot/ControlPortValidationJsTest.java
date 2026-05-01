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

class ControlPortValidationJsTest {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
    private static RuleFunction function;

    @BeforeAll
    static void setUp() {
        Path functionsDir = Path.of("src/test/resources/functions").toAbsolutePath();
        PolyglotFunctionProvider provider = PolyglotFunctionProvider.forDirectory(
                functionsDir, List.of("control-port-validation"));
        function = provider.functions().get(0);
    }

    @Test
    void shouldDetectMultipleControlAdapters() throws Exception {
        String yaml = """
                capability:
                  exposes:
                    - namespace: ctrl1
                      type: control
                      port: 9090
                    - namespace: ctrl2
                      type: control
                      port: 9091
                """;
        JsonNode root = YAML.readTree(yaml);
        List<String> results = function.evaluate(root, Map.of());
        assertFalse(results.isEmpty());
        assertTrue(results.get(0).contains("Only one control adapter"));
    }

    @Test
    void shouldDetectPortConflict() throws Exception {
        String yaml = """
                capability:
                  exposes:
                    - namespace: my-rest
                      type: rest
                      port: 8080
                    - namespace: ctrl
                      type: control
                      port: 8080
                """;
        JsonNode root = YAML.readTree(yaml);
        List<String> results = function.evaluate(root, Map.of());
        assertFalse(results.isEmpty());
        assertTrue(results.get(0).contains("conflicts"));
    }

    @Test
    void shouldPassWithSingleControlAndUniquePorts() throws Exception {
        String yaml = """
                capability:
                  exposes:
                    - namespace: my-rest
                      type: rest
                      port: 8080
                    - namespace: ctrl
                      type: control
                      port: 9090
                """;
        JsonNode root = YAML.readTree(yaml);
        List<String> results = function.evaluate(root, Map.of());
        assertTrue(results.isEmpty());
    }

    @Test
    void shouldPassWithNoControlAdapter() throws Exception {
        String yaml = """
                capability:
                  exposes:
                    - namespace: my-rest
                      type: rest
                      port: 8080
                """;
        JsonNode root = YAML.readTree(yaml);
        List<String> results = function.evaluate(root, Map.of());
        assertTrue(results.isEmpty());
    }
}
