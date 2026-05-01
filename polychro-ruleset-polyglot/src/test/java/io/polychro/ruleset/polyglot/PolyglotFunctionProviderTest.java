package io.polychro.ruleset.polyglot;

import io.polychro.ruleset.FunctionProvider;
import io.polychro.ruleset.RuleFunction;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.*;

class PolyglotFunctionProviderTest {

    private static final Path FUNCTIONS_DIR = Path.of("src/test/resources/functions").toAbsolutePath();

    @Test
    void functionsShouldReturnEmptyForDefaultConstructor() {
        PolyglotFunctionProvider provider = new PolyglotFunctionProvider();
        List<RuleFunction> functions = provider.functions();
        assertTrue(functions.isEmpty());
    }

    @Test
    void functionsShouldLoadJsFunctions() {
        PolyglotFunctionProvider provider = PolyglotFunctionProvider.forDirectory(
                FUNCTIONS_DIR, List.of("simple-check", "multi-result"));
        List<RuleFunction> functions = provider.functions();

        assertEquals(2, functions.size());
    }

    @Test
    void functionsShouldLoadNaftikoFunctions() {
        PolyglotFunctionProvider provider = PolyglotFunctionProvider.forDirectory(
                FUNCTIONS_DIR, List.of("unique-namespaces", "control-port-validation", "aggregate-semantics-consistency"));
        List<RuleFunction> functions = provider.functions();

        assertEquals(3, functions.size());
    }

    @Test
    void functionsShouldSkipUnknownFunctionName() {
        PolyglotFunctionProvider provider = PolyglotFunctionProvider.forDirectory(
                FUNCTIONS_DIR, List.of("nonexistent-func"));
        List<RuleFunction> functions = provider.functions();

        assertTrue(functions.isEmpty());
    }

    @Test
    void functionsShouldReturnEmptyWhenFunctionNamesEmpty() {
        PolyglotFunctionProvider provider = PolyglotFunctionProvider.forDirectory(FUNCTIONS_DIR, List.of());
        List<RuleFunction> functions = provider.functions();
        assertTrue(functions.isEmpty());
    }

    @Test
    void providerShouldBeDiscoverableViaServiceLoader() {
        ServiceLoader<FunctionProvider> loader = ServiceLoader.load(FunctionProvider.class);
        boolean found = false;
        for (FunctionProvider provider : loader) {
            if (provider instanceof PolyglotFunctionProvider) {
                found = true;
                break;
            }
        }
        assertTrue(found, "PolyglotFunctionProvider should be discoverable via ServiceLoader");
    }
}
