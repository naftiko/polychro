package io.polychro.wellformedness;

import io.polychro.spi.Validator;
import io.polychro.spi.ValidatorConfig;
import io.polychro.spi.ValidatorFactory;

/**
 * Factory for creating {@link WellformednessValidator} instances.
 */
public class WellformednessValidatorFactory implements ValidatorFactory {

    @Override
    public String name() {
        return WellformednessValidator.NAME;
    }

    @Override
    public Validator create(ValidatorConfig config) {
        int maxDepth = config.get("maxDepth", Integer.class)
                .orElse(WellformednessValidator.DEFAULT_MAX_DEPTH);
        int maxSize = config.get("maxSize", Integer.class)
                .orElse(WellformednessValidator.DEFAULT_MAX_SIZE);
        return new WellformednessValidator(maxDepth, maxSize);
    }
}
