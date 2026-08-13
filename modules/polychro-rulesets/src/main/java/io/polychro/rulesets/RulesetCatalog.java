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
package io.polychro.rulesets;

import io.polychro.ruleset.Ruleset;
import io.polychro.ruleset.RulesetParser;
import io.polychro.ruleset.utils.FileUtils;

import java.io.IOException;
import java.util.List;

/**
 * Provides access to the curated rulesets bundled in this module.
 */
public final class RulesetCatalog {

    private static final String BASE = "/rulesets";

    private static final List<String> AVAILABLE = List.of(
            "governance", "ai-safety", "security", "mcp", "consistency", "resilience", "agents");

    private static final RulesetParser RULESET_PARSER = new RulesetParser();

    private RulesetCatalog() {
    }

    /**
     * Returns the list of available ruleset names.
     */
    public static List<String> available() {
        return AVAILABLE;
    }

    /**
     * Loads a ruleset by name and returns its YAML content.
     * In case the ruleset has custom functions, use the
     * more capable {@link RulesetCatalog#loadAsRuleset(String)}.
     *
     * @param name the ruleset name (e.g. "governance", "ai-safety")
     * @return the YAML content of the ruleset
     * @throws IllegalArgumentException if the name is not a known ruleset
     */
    public static String load(String name) {
        if (!AVAILABLE.contains(name)) {
            throw new IllegalArgumentException("Unknown ruleset: " + name
                    + ". Available: " + AVAILABLE);
        }

        String resource = BASE + "/" + name + ".yml";
        try {
            return FileUtils.getFileContentFromClasspath(resource);
        } catch (IOException e) {
            throw new java.io.UncheckedIOException("Failed to read ruleset: " + resource, e);
        }
    }

    /**
     * Loads a ruleset by name and returns its {@link Ruleset} representation.
     *
     * @param name the ruleset name (e.g. "governance", "ai-safety")
     * @return the ruleset instance
     * @throws IllegalArgumentException if the name is not a known ruleset
     */
    public static Ruleset loadAsRuleset(String name) {
        if (!AVAILABLE.contains(name)) {
            throw new IllegalArgumentException("Unknown ruleset: " + name
                    + ". Available: " + AVAILABLE);
        }
        String resource = BASE + "/" + name + ".yml";
        return RULESET_PARSER.parse(resource, BASE, RulesetParser.RulesetSource.CLASSPATH);
    }
}
