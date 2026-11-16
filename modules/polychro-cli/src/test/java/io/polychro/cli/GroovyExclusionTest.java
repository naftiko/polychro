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

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Guards the CLI's runtime classpath, which is what the jar + JRE package ships, against Apache
 * Groovy. It comes transitively through {@code polychro-capability} → {@code
 * io.ikanos:ikanos-engine} and the CLI never runs it; it would add about 7 MB to the package.
 */
class GroovyExclusionTest {

  @Test
  void groovyMustNotBeOnTheCliClasspath() {
    assertThrows(
        ClassNotFoundException.class,
        () -> Class.forName("groovy.lang.GroovyObject", false,
            GroovyExclusionTest.class.getClassLoader()),
        "Apache Groovy is on the polychro-cli classpath. The CLI never runs it and it adds about"
            + " 7 MB to the jar + JRE package. Keep the org.apache.groovy:groovy exclusion on the"
            + " polychro-capability dependency in polychro-cli/pom.xml.");
  }
}
