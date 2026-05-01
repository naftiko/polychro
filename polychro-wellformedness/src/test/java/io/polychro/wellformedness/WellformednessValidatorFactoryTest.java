package io.polychro.wellformedness;

import io.polychro.spi.Validator;
import io.polychro.spi.ValidatorConfig;
import io.polychro.spi.ValidatorFactory;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.*;

class WellformednessValidatorFactoryTest {

    private final WellformednessValidatorFactory factory = new WellformednessValidatorFactory();

    @Test
    void nameShouldReturnWellformedness() {
        assertEquals("wellformedness", factory.name());
    }

    @Test
    void createShouldReturnValidatorWithDefaults() {
        ValidatorConfig config = new ValidatorConfig(Map.of());
        Validator validator = factory.create(config);
        assertNotNull(validator);
        assertEquals("wellformedness", validator.name());
    }

    @Test
    void createShouldRespectCustomMaxDepth() {
        ValidatorConfig config = new ValidatorConfig(Map.of("maxDepth", 5));
        Validator validator = factory.create(config);
        assertNotNull(validator);
        assertInstanceOf(WellformednessValidator.class, validator);
    }

    @Test
    void createShouldRespectCustomMaxSize() {
        ValidatorConfig config = new ValidatorConfig(Map.of("maxSize", 50));
        Validator validator = factory.create(config);
        assertNotNull(validator);
        assertInstanceOf(WellformednessValidator.class, validator);
    }

    @Test
    void createShouldRespectBothCustomLimits() {
        ValidatorConfig config = new ValidatorConfig(Map.of("maxDepth", 5, "maxSize", 50));
        Validator validator = factory.create(config);
        assertNotNull(validator);
    }

    @Test
    void serviceLoaderShouldDiscoverFactory() {
        ServiceLoader<ValidatorFactory> loader = ServiceLoader.load(ValidatorFactory.class);
        boolean found = false;
        for (ValidatorFactory f : loader) {
            if ("wellformedness".equals(f.name())) {
                found = true;
                break;
            }
        }
        assertTrue(found, "ServiceLoader should discover WellformednessValidatorFactory");
    }
}
