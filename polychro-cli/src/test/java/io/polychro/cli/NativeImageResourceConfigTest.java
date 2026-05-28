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
package io.polychro.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the GraalVM native-image resource-config.json against missing ResourceBundle patterns.
 *
 * <p>Running {@code mvn test} (JVM) cannot reproduce native-image runtime failures caused by
 * missing ResourceBundle declarations — those only manifest in the compiled binary. This test
 * asserts that every required pattern is present in the config file so that accidental removal
 * is caught at build time.
 *
 * <p>Background: {@code com.networknt:json-schema-validator} loads its i18n bundle via
 * {@code ResourceBundle.getBundle("jsv-messages")} in a static initializer. GraalVM does not
 * include classpath {@code *.properties} files in the native image unless they are declared in
 * {@code resource-config.json}. Without the {@code jsv-messages.*\\.properties} pattern, the
 * published binary crashes immediately with {@code MissingResourceException}.
 */
class NativeImageResourceConfigTest {

  private static final String RESOURCE_CONFIG =
      "META-INF/native-image/io.polychro/polychro-cli/resource-config.json";

  /**
   * All patterns that MUST be present in {@code resource-config.json} for the native binary to
   * work correctly. Add an entry here whenever a new library that loads ResourceBundles or other
   * classpath resources at runtime is added to the CLI's shaded jar.
   */
  private static final List<String> REQUIRED_PATTERNS =
      List.of(
          "META-INF/services/io.polychro.spi.ValidatorFactory",
          "META-INF/services/io.polychro.ruleset.FunctionProvider",
          "rulesets/.*\\.yml",
          "version\\.properties",
          // com.networknt:json-schema-validator loads this bundle in I18nSupport.<clinit>.
          // Without this pattern the native binary crashes with MissingResourceException.
          "jsv-messages.*\\.properties");

  @Test
  void resourceConfigShouldDeclareAllRequiredPatterns() throws Exception {
    List<String> declaredPatterns = readDeclaredPatterns();

    for (String required : REQUIRED_PATTERNS) {
      assertTrue(
          declaredPatterns.contains(required),
          "resource-config.json is missing required pattern: \""
              + required
              + "\". Declared patterns: "
              + declaredPatterns);
    }
  }

  private List<String> readDeclaredPatterns() throws Exception {
    InputStream in = getClass().getClassLoader().getResourceAsStream(RESOURCE_CONFIG);
    if (in == null) {
      throw new IllegalStateException("Cannot find " + RESOURCE_CONFIG + " on classpath");
    }
    ObjectMapper mapper = new ObjectMapper();
    JsonNode root = mapper.readTree(in);
    JsonNode includes = root.path("resources").path("includes");
    List<String> patterns = new ArrayList<>();
    for (JsonNode include : includes) {
      String pattern = include.path("pattern").asText(null);
      if (pattern != null) {
        patterns.add(pattern);
      }
    }
    return patterns;
  }
}
