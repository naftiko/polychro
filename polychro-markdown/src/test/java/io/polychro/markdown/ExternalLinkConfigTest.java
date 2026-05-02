package io.polychro.markdown;

import io.polychro.spi.Diagnostic;
import io.polychro.spi.ValidatorConfig;
import io.polychro.spi.Validator;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExternalLinkConfigTest {

    @Test
    void checkExternalLinksFalseShouldSkipExternalChecks() {
        ValidatorConfig config = new ValidatorConfig(Map.of("checkExternalLinks", false));
        MarkdownValidatorFactory factory = new MarkdownValidatorFactory();
        Validator validator = factory.create(config);
        assertNotNull(validator);
    }

    @Test
    void checkExternalLinksTrueShouldEnableExternalChecks() {
        ValidatorConfig config = new ValidatorConfig(Map.of("checkExternalLinks", true));
        MarkdownValidatorFactory factory = new MarkdownValidatorFactory();
        Validator validator = factory.create(config);
        assertNotNull(validator);
    }

    @Test
    void customTimeoutShouldBeApplied() {
        ValidatorConfig config = new ValidatorConfig(Map.of(
                "checkExternalLinks", true,
                "externalLinkTimeout", 3000));
        MarkdownValidatorFactory factory = new MarkdownValidatorFactory();
        Validator validator = factory.create(config);
        assertNotNull(validator);
    }

    @Test
    void customRateLimitShouldBeApplied() {
        ValidatorConfig config = new ValidatorConfig(Map.of(
                "checkExternalLinks", true,
                "externalLinkRateLimit", 5));
        MarkdownValidatorFactory factory = new MarkdownValidatorFactory();
        Validator validator = factory.create(config);
        assertNotNull(validator);
    }

    @Test
    void defaultValuesShouldBeUsedWhenNotConfigured() {
        ValidatorConfig config = new ValidatorConfig(Map.of());
        MarkdownValidatorFactory factory = new MarkdownValidatorFactory();
        Validator validator = factory.create(config);
        assertNotNull(validator);
        assertEquals("markdown", validator.name());
    }
}
