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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Guards the CLI's runtime classpath, which is what the jar + JRE package ships, against the unused
 * GraalPy (Truffle Python) stack.
 *
 * <p>The CLI only uses the GraalVM <strong>JS</strong> language (via {@code
 * polychro-ruleset-polyglot} → {@code org.graalvm.js:js-community}). GraalPy is pulled in purely
 * transitively through {@code polychro-capability} → {@code io.ikanos:ikanos-engine} → {@code
 * org.graalvm.polyglot:python}. It is never executed by Polychro.
 *
 * <p>With GraalPy, the package's {@code lib/} grows from about 95 MB to about 254 MB: {@code
 * python-language} (~86 MB), {@code python-resources} (~61 MB), BouncyCastle and the Truffle NFI
 * and LLVM jars, which every install downloads and unpacks for nothing.
 *
 * <p>This test fails at {@code mvn test} time if GraalPy ever returns to the CLI's runtime
 * classpath.
 */
class GraalPyExclusionTest {

  /** A GraalPy language class that exists only in {@code org.graalvm.python:python-language}. */
  private static final String GRAALPY_LANGUAGE_CLASS = "com.oracle.graal.python.PythonLanguage";

  /** The GraalVM JS language class that Polychro actually depends on — must remain available. */
  private static final String GRAALJS_LANGUAGE_CLASS = "com.oracle.truffle.js.lang.JavaScriptLanguage";

  @Test
  void graalPyMustNotBeOnTheCliClasspath() {
    assertEquals(
        false,
        isClassPresent(GRAALPY_LANGUAGE_CLASS),
        "GraalPy ("
            + GRAALPY_LANGUAGE_CLASS
            + ") is on the polychro-cli classpath. It is unused by the CLI and adds about 159 MB "
            + "to the jar + JRE package. Keep the org.graalvm.polyglot:python exclusion on the "
            + "polychro-capability dependency in polychro-cli/pom.xml.");
  }

  @Test
  void graalJsMustRemainOnTheCliClasspath() {
    assertNotNull(
        loadClassOrNull(GRAALJS_LANGUAGE_CLASS),
        "GraalJS ("
            + GRAALJS_LANGUAGE_CLASS
            + ") must stay on the classpath: it is the only polyglot language Polychro uses. "
            + "If this fails, the python exclusion was too broad and removed JS as well.");
  }

  private static boolean isClassPresent(String className) {
    return loadClassOrNull(className) != null;
  }

  private static Class<?> loadClassOrNull(String className) {
    try {
      return Class.forName(className, false, GraalPyExclusionTest.class.getClassLoader());
    } catch (ClassNotFoundException | NoClassDefFoundError e) {
      return null;
    }
  }
}
