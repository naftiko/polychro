package io.polychro.spi;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ValidatorConfigTest {

    @Test
    void getShouldReturnValueWhenKeyExistsAndTypeMatches() {
        ValidatorConfig config = new ValidatorConfig(Map.of("schemaPath", "/path/to/schema.json"));
        Optional<String> result = config.get("schemaPath", String.class);
        assertTrue(result.isPresent());
        assertEquals("/path/to/schema.json", result.get());
    }

    @Test
    void getShouldReturnEmptyWhenKeyDoesNotExist() {
        ValidatorConfig config = new ValidatorConfig(Map.of("other", "value"));
        Optional<String> result = config.get("missing", String.class);
        assertTrue(result.isEmpty());
    }

    @Test
    void getShouldReturnEmptyWhenTypeDoesNotMatch() {
        ValidatorConfig config = new ValidatorConfig(Map.of("count", 42));
        Optional<String> result = config.get("count", String.class);
        assertTrue(result.isEmpty());
    }

    @Test
    void getShouldReturnEmptyWhenPropertiesIsNull() {
        ValidatorConfig config = new ValidatorConfig(null);
        Optional<String> result = config.get("key", String.class);
        assertTrue(result.isEmpty());
    }

    @Test
    void getShouldHandleIntegerType() {
        ValidatorConfig config = new ValidatorConfig(Map.of("maxDepth", 10));
        Optional<Integer> result = config.get("maxDepth", Integer.class);
        assertTrue(result.isPresent());
        assertEquals(10, result.get());
    }

    @Test
    void getShouldHandleBooleanType() {
        ValidatorConfig config = new ValidatorConfig(Map.of("strict", true));
        Optional<Boolean> result = config.get("strict", Boolean.class);
        assertTrue(result.isPresent());
        assertTrue(result.get());
    }

    @Test
    void propertiesShouldBeAccessible() {
        Map<String, Object> props = Map.of("a", "1", "b", 2);
        ValidatorConfig config = new ValidatorConfig(props);
        assertEquals(props, config.properties());
    }
}
