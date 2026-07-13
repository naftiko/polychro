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
import com.networknt.schema.ValidatorTypeCode;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards reflect-config.json against missing com.networknt.schema validator entries.
 *
 * <p>JsonMetaSchema instantiates validators via reflection (Class.forName + getConstructor +
 * newInstance) using the class stored in each ValidatorTypeCode enum constant. GraalVM
 * native-image requires every reflectively-instantiated class to be declared in
 * reflect-config.json; without it the binary fails at runtime with NoSuchMethodException.
 *
 * <p>This test reads ValidatorTypeCode.values() at JVM test time, extracts the concrete
 * validator class each constant holds, and asserts that each one is declared in
 * reflect-config.json. This way any upgrade of com.networknt:json-schema-validator that
 * adds a new validator keyword immediately breaks the build, rather than silently
 * producing a broken native binary.
 *
 * <p>NOTE: reflect-config.json is currently maintained by hand. When networknt is upgraded,
 * update the file to add missing entries (the failing test will tell you exactly which ones).
 * See issue TODO-networknt-reflect-config-generation for a plan to automate this via a
 * Maven code-generation step.
 */
class NetworkntValidatorReflectConfigTest {

  private static final String REFLECT_CONFIG =
      "META-INF/native-image/io.polychro/polychro-cli/reflect-config.json";

  @Test
  void reflectConfigShouldDeclareAllValidatorTypeCodeClasses() throws Exception {
    List<String> declaredClasses = readDeclaredClassNames();
    List<String> missing = new ArrayList<>();

    for (ValidatorTypeCode code : ValidatorTypeCode.values()) {
      Class<?> validatorClass = extractValidatorClass(code);
      if (validatorClass == null) {
        continue; // enum constant has no associated validator class (e.g. CROSS_EDITS, EDITS, ID)
      }
      String className = validatorClass.getName();
      if (!declaredClasses.contains(className)) {
        missing.add(className + " (from ValidatorTypeCode." + code.name() + ")");
      }
    }

    assertTrue(
        missing.isEmpty(),
        "reflect-config.json is missing entries for the following networknt validator classes."
            + " Add them with allDeclaredConstructors:true so GraalVM can instantiate them"
            + " at runtime.\nMissing:\n  - "
            + String.join("\n  - ", missing));
  }

  private Class<?> extractValidatorClass(ValidatorTypeCode code) throws Exception {
    // ValidatorTypeCode stores the Class<?> in a private field named "validator"
    for (Field f : ValidatorTypeCode.class.getDeclaredFields()) {
      if (f.getType().equals(Class.class)) {
        f.setAccessible(true);
        return (Class<?>) f.get(code);
      }
    }
    return null;
  }

  private List<String> readDeclaredClassNames() throws Exception {
    InputStream in = getClass().getClassLoader().getResourceAsStream(REFLECT_CONFIG);
    if (in == null) {
      throw new IllegalStateException("Cannot find " + REFLECT_CONFIG + " on classpath");
    }
    ObjectMapper mapper = new ObjectMapper();
    JsonNode root = mapper.readTree(in);
    List<String> names = new ArrayList<>();
    for (JsonNode entry : root) {
      String name = entry.path("name").asText(null);
      if (name != null) {
        names.add(name);
      }
    }
    return names;
  }
}
