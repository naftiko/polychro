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

import io.polychro.ruleset.Function;
import org.graalvm.polyglot.Engine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.polychro.ruleset.RulesetParser.SUPPORTED_SCRIPT_EXTENSIONS;

/**
 * Loads custom rule functions from polyglot script files.
 * <p>
 * Supports JavaScript (.js), Python (.py), and Groovy (.groovy) via GraalVM Polyglot API.
 * Each file must export a default function that accepts a target value and returns
 * an array of result objects with {@code message} and optional {@code path} fields.
 */
class PolyglotFunctionLoader {

    private static final Logger LOG = LoggerFactory.getLogger(PolyglotFunctionLoader.class);

    /**
     * Lazily initialized GraalVM Engine.
     * <p>
     * {@code Engine.create()} is deferred until the first script is actually loaded so that
     * a ruleset declaring {@code functions:} but whose custom rules never fire (and thus
     * {@link #loadFunctions} is never called with non-empty names) does not crash in
     * environments where no Truffle language is on the module-path (e.g. a misconfigured
     * native image). See: <a href="https://github.com/naftiko/polychro/issues/45">issue #45</a>.
     */
    private Engine engine;

    PolyglotFunctionLoader() {
        // Engine is created lazily on first use — see getEngine().
    }

    /**
     * Returns the shared {@link Engine}, creating it on first call.
     *
     * <p>Package-private to allow direct testing from {@code PolyglotFunctionLoaderTest}.
     */
    Engine getEngine() {
        if (engine == null) {
            engine = Engine.create();
        }
        return engine;
    }

    /**
     * Load functions from the given directory by name.
     *
     * @param functionsDir  the directory containing function script files
     * @param functionNames the function names to load (file stem must match)
     * @return a map of function name to loaded PolyglotRuleFunction
     */
    Map<String, PolyglotRuleFunction> loadFunctions(Path functionsDir, java.util.List<String> functionNames) {
        Map<String, PolyglotRuleFunction> functions = new LinkedHashMap<>();

        for (String name : functionNames) {
            Path scriptFile = resolveScriptFile(functionsDir, name);
            if (scriptFile == null) {
                LOG.warn("No script file found for function '{}' in {}", name, functionsDir);
                continue;
            }
            String languageId = detectLanguage(scriptFile.getFileName().toString());
            try {
                String source = Files.readString(scriptFile);
                if (source.isBlank()) {
                    LOG.warn("Empty script file for function '{}': {}", name, scriptFile);
                    continue;
                }
                PolyglotRuleFunction function = new PolyglotRuleFunction(name, source, languageId, getEngine());
                functions.put(name, function);
            } catch (IOException e) {
                LOG.warn("Failed to read script file for function '{}': {}", name, e.getMessage());
            }
        }

        return functions;
    }

    /**
     * Load functions from a map containing their content by filename.
     *
     * @param functions  the custom functions
     * @return a map of function names to loaded {@code PolyglotRuleFunction}s
     */
    Map<String, PolyglotRuleFunction> loadFunctions(List<Function> functions) {
        Map<String, PolyglotRuleFunction> polyglotFunctions = new LinkedHashMap<>();

        for (Function function : functions) {
            String languageId = detectLanguage(function.filename());
            if (languageId == null) {
                LOG.warn("Script missing extension: '{}'", function.filename());
                continue;
            }

            if (function.sourceCode().isBlank()) {
                LOG.warn("Empty script file for function '{}'", function.filename());
                continue;
            }

            PolyglotRuleFunction polyglotRuleFunction = new PolyglotRuleFunction(function.functionName(),
                    function.sourceCode(), languageId, getEngine());
            polyglotFunctions.put(function.functionName(), polyglotRuleFunction);
        }

        return polyglotFunctions;
    }

    Path resolveScriptFile(Path functionsDir, String name) {
        for (String ext : SUPPORTED_SCRIPT_EXTENSIONS) {
            Path candidate = functionsDir.resolve(name + ext);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    static String detectLanguage(String filename) {
        if (filename.endsWith(".js")) {
            return "js";
        }
        if (filename.endsWith(".py")) {
            return "python";
        }
        if (filename.endsWith(".groovy")) {
            return "groovy";
        }
        return null;
    }

    void close() {
        if (engine != null) {
            engine.close();
        }
    }
}
