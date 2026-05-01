package io.polychro.ruleset.polyglot;

import io.polychro.ruleset.FunctionProvider;
import io.polychro.ruleset.RuleFunction;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A {@link FunctionProvider} that loads custom functions from polyglot scripts
 * (JavaScript, Python, Groovy) via GraalVM Polyglot API.
 * <p>
 * Discovered via {@link java.util.ServiceLoader} registration.
 * Functions are loaded from the {@code functionsDir} declared in the ruleset.
 */
public class PolyglotFunctionProvider implements FunctionProvider {

    private final Path functionsDir;
    private final List<String> functionNames;

    public PolyglotFunctionProvider() {
        this.functionsDir = null;
        this.functionNames = List.of();
    }

    PolyglotFunctionProvider(Path functionsDir, List<String> functionNames) {
        this.functionsDir = functionsDir;
        this.functionNames = functionNames;
    }

    @Override
    public List<RuleFunction> functions() {
        if (functionsDir == null || functionNames.isEmpty()) {
            return List.of();
        }
        PolyglotFunctionLoader loader = new PolyglotFunctionLoader();
        Map<String, PolyglotRuleFunction> loaded = loader.loadFunctions(functionsDir, functionNames);
        return new ArrayList<>(loaded.values());
    }

    /**
     * Create a provider for specific functions directory and names.
     *
     * @param functionsDir  the directory containing function script files
     * @param functionNames the function names to load
     * @return a configured provider
     */
    public static PolyglotFunctionProvider forDirectory(Path functionsDir, List<String> functionNames) {
        return new PolyglotFunctionProvider(functionsDir, functionNames);
    }
}
