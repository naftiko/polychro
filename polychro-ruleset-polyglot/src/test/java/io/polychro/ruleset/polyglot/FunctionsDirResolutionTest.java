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

import io.polychro.ruleset.RulesetValidatorFactory;
import io.polychro.spi.Diagnostic;
import io.polychro.spi.Document;
import io.polychro.spi.Validator;
import io.polychro.spi.ValidatorConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test for issue #44: {@code functionsDir} must be resolved relative to the
 * ruleset file's parent directory, not the process CWD.
 *
 * <p>The test writes a ruleset file to a temp directory (with {@code functionsDir: ./functions})
 * alongside a {@code functions/} sub-directory containing the real {@code unique-namespaces.js}
 * script. It then loads the ruleset via an absolute path while the process CWD is a completely
 * different directory, verifying that the custom JS function is still discovered and fires.
 */
class FunctionsDirResolutionTest {

    /**
     * The actual JS source file from the test resources, reused here to avoid duplication.
     * Resolved via the classloader so the path is CWD-independent (mirrors the fix in #44).
     */
    private static final Path SOURCE_FUNCTIONS_DIR;

    static {
        try {
            SOURCE_FUNCTIONS_DIR = Path.of(
                    FunctionsDirResolutionTest.class
                            .getClassLoader()
                            .getResource("functions")
                            .toURI());
        } catch (URISyntaxException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static final String DOCUMENT_WITH_DUPLICATE_NAMESPACE = """
            consumes:
              - namespace: dup
                baseUri: https://a.example.com
              - namespace: dup
                baseUri: https://b.example.com
            """;

    @Test
    void functionsDirShouldResolveRelativeToRulesetFileNotCwd(@TempDir Path tempDir) throws Exception {
        // Arrange: create a sub-directory structure in the temp dir:
        //   <tempDir>/rules/my-ruleset.yml
        //   <tempDir>/rules/functions/unique-namespaces.js
        Path rulesDir = tempDir.resolve("rules");
        Path functionsDir = rulesDir.resolve("functions");
        Files.createDirectories(functionsDir);

        // Copy the unique-namespaces.js script from the test resources
        Path jsSource = SOURCE_FUNCTIONS_DIR.resolve("unique-namespaces.js");
        Files.copy(jsSource, functionsDir.resolve("unique-namespaces.js"));

        // Ruleset uses a RELATIVE functionsDir (the bug scenario)
        String rulesetContent = """
                functionsDir: ./functions
                functions:
                  - unique-namespaces
                rules:
                  naftiko-unique-namespaces:
                    description: Namespaces must be globally unique.
                    severity: error
                    given: $
                    then:
                      function: unique-namespaces
                """;
        Path rulesetFile = rulesDir.resolve("my-ruleset.yml");
        Files.writeString(rulesetFile, rulesetContent);

        // The process CWD is tempDir itself — NOT the rules/ sub-directory.
        // Before the fix, Path.of("./functions") resolves to <tempDir>/functions (absent),
        // causing the 8× "No script file found" warning and zero custom diagnostics.
        // After the fix, it must resolve to <tempDir>/rules/functions (present).
        Validator validator = new RulesetValidatorFactory()
                .create(new ValidatorConfig(Map.of("rulesetPath", rulesetFile.toString())));

        Document doc = Document.fromString(DOCUMENT_WITH_DUPLICATE_NAMESPACE, "yaml");

        // Act
        List<Diagnostic> diagnostics = validator.validate(doc);

        // Assert: the custom JS function must fire even though the CWD ≠ ruleset directory
        assertFalse(diagnostics.isEmpty(),
                "custom JS function 'unique-namespaces' must fire when ruleset is loaded by "
                        + "absolute path from a different CWD (issue #44: functionsDir must be "
                        + "resolved relative to the ruleset file, not the process CWD)");
        assertTrue(diagnostics.stream().anyMatch(d -> d.code().equals("naftiko-unique-namespaces")),
                "diagnostic code must be 'naftiko-unique-namespaces'");
    }
}
