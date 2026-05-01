package io.polychro.ruleset;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class BuiltinFunctionsTest {

    @Test
    void registryShouldContainAllTwelveBuiltins() {
        List<String> names = BuiltinFunctions.names();
        assertTrue(names.contains("truthy"));
        assertTrue(names.contains("falsy"));
        assertTrue(names.contains("defined"));
        assertTrue(names.contains("undefined"));
        assertTrue(names.contains("pattern"));
        assertTrue(names.contains("enumeration"));
        assertTrue(names.contains("length"));
        assertTrue(names.contains("schema"));
        assertTrue(names.contains("alphabetical"));
        assertTrue(names.contains("casing"));
        assertTrue(names.contains("xor"));
        assertTrue(names.contains("typedEnum"));
    }

    @Test
    void registryShouldDiscoverCustomFunctionViaServiceLoader() {
        Optional<RuleFunction> fn = BuiltinFunctions.get("testCustomFunction");
        assertTrue(fn.isPresent());
        assertEquals("testCustomFunction", fn.get().name());
    }

    @Test
    void getShouldReturnFunctionByName() {
        Optional<RuleFunction> fn = BuiltinFunctions.get("truthy");
        assertTrue(fn.isPresent());
        assertEquals("truthy", fn.get().name());
    }

    @Test
    void getShouldReturnEmptyForUnknown() {
        Optional<RuleFunction> fn = BuiltinFunctions.get("nonexistent");
        assertTrue(fn.isEmpty());
    }

    @Test
    void getShouldBeCaseSensitive() {
        Optional<RuleFunction> fn = BuiltinFunctions.get("Truthy");
        assertTrue(fn.isEmpty());
    }

    @Test
    void getShouldReturnEachFunction() {
        for (String name : BuiltinFunctions.names()) {
            Optional<RuleFunction> fn = BuiltinFunctions.get(name);
            assertTrue(fn.isPresent(), "Expected function: " + name);
            assertEquals(name, fn.get().name());
        }
    }
}
