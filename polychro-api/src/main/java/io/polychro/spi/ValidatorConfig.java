package io.polychro.spi;

import java.util.Map;
import java.util.Optional;

/**
 * Type-safe configuration map for a validator.
 *
 * @param properties the raw configuration properties
 */
public record ValidatorConfig(Map<String, Object> properties) {

    /**
     * Retrieve a configuration value by key, cast to the expected type.
     *
     * @param key  the configuration key
     * @param type the expected value type
     * @param <T>  the type parameter
     * @return the value if present and of the correct type, otherwise empty
     */
    public <T> Optional<T> get(String key, Class<T> type) {
        if (properties == null) {
            return Optional.empty();
        }
        Object value = properties.get(key);
        if (value == null) {
            return Optional.empty();
        }
        if (type.isInstance(value)) {
            return Optional.of(type.cast(value));
        }
        return Optional.empty();
    }
}
