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
