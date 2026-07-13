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
package io.polychro.ruleset;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RulesetMergingTest {

    private final RulesetParser parser = new RulesetParser();
    private final RulesetComposer composer = new RulesetComposer(parser);
    private static final Path EXTENDS_DIR = Path.of("src/test/resources/fixtures/extends").toAbsolutePath();

    @Test
    void sameNameRuleInChildShouldReplaceParent() {
        Ruleset parent = new Ruleset(List.of(), Map.of(), List.of(), List.of(), null, List.of(),
                Map.of("shared-rule", new Rule("shared-rule", "Parent message", null, "warn", true,
                        null, null, List.of("$.info"), List.of(new RuleAction(null, "truthy", Map.of())))),
                null);

        Rule childRule = new Rule("shared-rule", "Child message", null, "error", true,
                null, null, List.of("$.info.name"), List.of(new RuleAction(null, "truthy", Map.of())));

        // Simulate merging manually — same logic as RulesetComposer
        Map<String, Rule> merged = new LinkedHashMap<>(parent.rules());
        merged.put("shared-rule", childRule);

        assertEquals("Child message", merged.get("shared-rule").message());
        assertEquals("error", merged.get("shared-rule").severity());
        assertEquals(List.of("$.info.name"), merged.get("shared-rule").given());
    }

    @Test
    void parentRuleAbsentInChildShouldBeInheritedAsIs() {
        String rulesetPath = EXTENDS_DIR.resolve("child-ruleset.yml").toString();
        Ruleset child = parser.parse(Path.of(rulesetPath));
        Ruleset composed = composer.compose(child, EXTENDS_DIR);

        // parent-name-truthy and parent-desc-truthy should be inherited
        assertTrue(composed.rules().containsKey("parent-name-truthy"));
        assertTrue(composed.rules().containsKey("parent-desc-truthy"));
        assertEquals("Name must not be empty", composed.rules().get("parent-name-truthy").message());
    }

    @Test
    void bareOffSeverityInChildShouldDisableInheritedRule() {
        String rulesetPath = EXTENDS_DIR.resolve("override-severity-child.yml").toString();
        Ruleset child = parser.parse(Path.of(rulesetPath));
        Ruleset composed = composer.compose(child, EXTENDS_DIR, rulesetPath);

        // parent-desc-truthy is disabled with bare "severity: off"
        Rule disabledRule = composed.rules().get("parent-desc-truthy");
        assertNotNull(disabledRule);
        assertEquals("off", disabledRule.severity());
        assertTrue(disabledRule.given().isEmpty());
        assertTrue(disabledRule.then().isEmpty());
    }

    @Test
    void composeWithNullSourcePathShouldWork() {
        String rulesetPath = EXTENDS_DIR.resolve("child-ruleset.yml").toString();
        Ruleset child = parser.parse(Path.of(rulesetPath));
        Ruleset composed = composer.compose(child, EXTENDS_DIR, null);

        assertTrue(composed.rules().containsKey("parent-name-truthy"));
    }

    @Test
    void composeNoBaseDirShouldUseCurrentDir() {
        // Ruleset with no extends should pass through compose(Ruleset) unchanged
        Ruleset simple = new Ruleset(List.of(), Map.of(), List.of(), List.of(), null, List.of(),
                Map.of("rule", new Rule("rule", "msg", null, "warn", true,
                        null, null, List.of("$"), List.of(new RuleAction(null, "truthy", Map.of())))),
                null);
        Ruleset composed = composer.compose(simple);
        assertEquals(1, composed.rules().size());
    }

    @Test
    void offSeverityWithNonEmptyGivenShouldNotTriggerBareOff() {
        // Child has severity "off" but also has given — should NOT be treated as bare disable
        Rule offWithGiven = new Rule("parent-name-truthy", null, null, "off", true,
                null, null, List.of("$.info.name"), List.of(new RuleAction(null, "truthy", Map.of())));
        Ruleset childWithOff = new Ruleset(List.of("parent-ruleset.yml"), Map.of(), List.of(),
                List.of(), null, List.of(), Map.of("parent-name-truthy", offWithGiven), null);
        Ruleset composed = composer.compose(childWithOff, EXTENDS_DIR, null);

        // Should use the child rule as-is (not bare disable)
        Rule result = composed.rules().get("parent-name-truthy");
        assertEquals("off", result.severity());
        assertEquals(List.of("$.info.name"), result.given());
    }

    @Test
    void offSeverityWithNonEmptyThenShouldNotTriggerBareOff() {
        // Child has severity "off" and empty given but non-empty then
        Rule offWithThen = new Rule("parent-name-truthy", null, null, "off", true,
                null, null, List.of(), List.of(new RuleAction(null, "truthy", Map.of())));
        Ruleset childWithOff = new Ruleset(List.of("parent-ruleset.yml"), Map.of(), List.of(),
                List.of(), null, List.of(), Map.of("parent-name-truthy", offWithThen), null);
        Ruleset composed = composer.compose(childWithOff, EXTENDS_DIR, null);

        // Should use the child rule as-is (not bare disable)
        Rule result = composed.rules().get("parent-name-truthy");
        assertEquals("off", result.severity());
        assertFalse(result.then().isEmpty());
    }

    @Test
    void childRecommendedOverrideShouldApply() {
        Rule parentRule = new Rule("test-rule", "Test", null, "warn", true,
                null, null, List.of("$"), List.of(new RuleAction(null, "truthy", Map.of())));
        Rule childRule = new Rule("test-rule", "Test", null, "warn", false,
                null, null, List.of("$"), List.of(new RuleAction(null, "truthy", Map.of())));

        Map<String, Rule> merged = new LinkedHashMap<>();
        merged.put("test-rule", parentRule);
        merged.put("test-rule", childRule); // child overrides

        assertFalse(merged.get("test-rule").recommended());
    }

    @Test
    void childGivenOverrideShouldApply() {
        Rule parentRule = new Rule("test-rule", "Test", null, "warn", true,
                null, null, List.of("$.old"), List.of(new RuleAction(null, "truthy", Map.of())));
        Rule childRule = new Rule("test-rule", "Test", null, "warn", true,
                null, null, List.of("$.new.path"), List.of(new RuleAction(null, "truthy", Map.of())));

        Map<String, Rule> merged = new LinkedHashMap<>();
        merged.put("test-rule", parentRule);
        merged.put("test-rule", childRule);

        assertEquals(List.of("$.new.path"), merged.get("test-rule").given());
    }

    @Test
    void childThenOverrideShouldApply() {
        Rule parentRule = new Rule("test-rule", "Test", null, "warn", true,
                null, null, List.of("$"),
                List.of(new RuleAction(null, "truthy", Map.of())));
        Rule childRule = new Rule("test-rule", "Test", null, "warn", true,
                null, null, List.of("$"),
                List.of(new RuleAction("field", "pattern", Map.of("match", "^[a-z]"))));

        Map<String, Rule> merged = new LinkedHashMap<>();
        merged.put("test-rule", parentRule);
        merged.put("test-rule", childRule);

        assertEquals("pattern", merged.get("test-rule").then().get(0).functionName());
        assertEquals("field", merged.get("test-rule").then().get(0).field());
    }
}
