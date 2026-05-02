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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Provides access to the curated rulesets bundled in this module.
 * Each method returns the YAML content as a string, suitable for passing
 * to {@code ValidatorConfig} via the {@code rulesetContent} key.
 */
public final class RulesetCatalog {

    private static final String BASE = "/rulesets/";

    private static final List<String> AVAILABLE = List.of(
            "governance", "ai-safety", "security", "mcp", "consistency", "resilience");

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
        String resource = BASE + name + ".yml";
        try (InputStream is = RulesetCatalog.class.getResourceAsStream(resource)) {
            if (is == null) {
                throw new IllegalStateException("Ruleset resource not found: " + resource);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException("Failed to read ruleset: " + resource, e);
        }
    }

    /**
     * Returns the classpath resource path for a ruleset name.
     *
     * @param name the ruleset name
     * @return the classpath resource path (e.g. "/rulesets/governance.yml")
     */
    public static String resourcePath(String name) {
        if (!AVAILABLE.contains(name)) {
            throw new IllegalArgumentException("Unknown ruleset: " + name
                    + ". Available: " + AVAILABLE);
        }
        return BASE + name + ".yml";
    }
}
