package io.polychro.spi;

/**
 * Factory for creating {@link Validator} instances.
 * <p>
 * Implementations are discovered via {@link java.util.ServiceLoader}.
 * Each module that provides a validator registers its factory in
 * {@code META-INF/services/io.polychro.spi.ValidatorFactory}.
 */
public interface ValidatorFactory {

    /**
     * @return the unique name of the validator this factory creates
     */
    String name();

    /**
     * Create a new validator instance with the given configuration.
     *
     * @param config per-validator configuration properties
     * @return a configured validator instance
     */
    Validator create(ValidatorConfig config);
}
