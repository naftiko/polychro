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
import org.graalvm.polyglot.Engine;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PolyglotResourceLimitsTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static Engine engine;

    @BeforeAll
    static void setUp() {
        engine = Engine.create();
    }

    @AfterAll
    static void tearDown() {
        engine.close();
    }

    @Test
    void jsInfiniteLoopShouldBeTerminated() throws Exception {
        String source = """
                export default function infiniteLoop(targetVal) {
                  while (true) {}
                  return [];
                }
                """;
        PolyglotRuleFunction fn = new PolyglotRuleFunction("infinite-loop", source, "js", engine);
        JsonNode input = JSON.readTree("{}");

        List<String> results = fn.evaluate(input, Map.of());
        assertFalse(results.isEmpty());
        assertTrue(results.get(0).contains("execution failed"));
    }
}
