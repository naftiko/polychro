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

import org.graalvm.polyglot.Engine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

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
     * Lazily initialised GraalVM Engine.
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
            String languageId = detectLanguage(scriptFile);
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

    Path resolveScriptFile(Path functionsDir, String name) {
        for (String ext : new String[]{".js", ".py", ".groovy"}) {
            Path candidate = functionsDir.resolve(name + ext);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    static String detectLanguage(Path scriptFile) {
        String fileName = scriptFile.getFileName().toString();
        if (fileName.endsWith(".js")) {
            return "js";
        }
        if (fileName.endsWith(".py")) {
            return "python";
        }
        if (fileName.endsWith(".groovy")) {
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
