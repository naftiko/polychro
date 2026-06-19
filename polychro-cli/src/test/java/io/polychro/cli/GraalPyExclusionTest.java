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
 * Guards the CLI's native-image classpath against the unused GraalPy (Truffle Python) stack.
 *
 * <p>The CLI only uses the GraalVM <strong>JS</strong> language (via {@code
 * polychro-ruleset-polyglot} → {@code org.graalvm.js:js-community}). GraalPy is pulled in purely
 * transitively through {@code polychro-capability} → {@code io.ikanos:ikanos-engine} → {@code
 * org.graalvm.polyglot:python}. It is never executed by Polychro.
 *
 * <p>When GraalPy is on the native-image classpath, the {@code python-language} (~88 MB) and {@code
 * python-resources} (~62 MB) jars plus the full Truffle/LLVM/BouncyCastle closure push the
 * native-image analysis to ~343,000 reachable methods. On the memory-constrained GitHub {@code
 * macos-latest} runner (7 GB RAM) the build spends ~79% of its time in GC and is aborted by the
 * deadlock watchdog (exit status 30), so the macOS CLI binary is never produced
 * (naftiko/polychro#58).
 *
 * <p>This test fails fast — at {@code mvn test} time, on every platform — if GraalPy ever returns
 * to the CLI's runtime classpath, instead of waiting ~1h26 for the macOS native build to OOM.
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
            + ") is on the polychro-cli classpath. It is unused by the CLI and bloats the "
            + "native-image analysis until the macOS build OOMs (naftiko/polychro#58). Keep the "
            + "org.graalvm.polyglot:python exclusion on the polychro-capability dependency in "
            + "polychro-cli/pom.xml.");
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
