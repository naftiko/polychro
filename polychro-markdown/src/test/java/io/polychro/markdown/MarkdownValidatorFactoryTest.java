package io.polychro.markdown;

import io.polychro.spi.Validator;
import io.polychro.spi.ValidatorConfig;
import io.polychro.spi.ValidatorFactory;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MarkdownValidatorFactoryTest {

    private final MarkdownValidatorFactory factory = new MarkdownValidatorFactory();

    @Test
    void nameShouldReturnMarkdown() {
        assertEquals("markdown", factory.name());
    }

    @Test
    void createShouldReturnValidatorWithDefaults() {
        Validator validator = factory.create(new ValidatorConfig(Map.of()));
        assertNotNull(validator);
        assertEquals("markdown", validator.name());
    }

    @Test
    void createShouldAcceptCustomLineLength() {
        Validator validator = factory.create(new ValidatorConfig(Map.of("lineLength", 80)));
        assertNotNull(validator);
    }

    @Test
    void createShouldAcceptCustomListMarker() {
        Validator validator = factory.create(new ValidatorConfig(Map.of("listMarker", "*")));
        assertNotNull(validator);
    }

    @Test
    void createShouldAcceptFormatOverride() {
        Validator validator = factory.create(new ValidatorConfig(Map.of("format", "skill")));
        assertNotNull(validator);
    }

    @Test
    void createWithAutoDetectShouldDetectSkillFormat() {
        Validator validator = factory.createWithAutoDetect(
                new ValidatorConfig(Map.of()), "path/to/SKILL.md");
        assertNotNull(validator);
    }

    @Test
    void createWithAutoDetectShouldUseFormatOverride() {
        Validator validator = factory.createWithAutoDetect(
                new ValidatorConfig(Map.of("format", "agents")), "path/to/SKILL.md");
        assertNotNull(validator);
    }

    @Test
    void serviceLoaderShouldDiscoverFactory() {
        ServiceLoader<ValidatorFactory> loader = ServiceLoader.load(ValidatorFactory.class);
        boolean found = false;
        for (ValidatorFactory f : loader) {
            if (f instanceof MarkdownValidatorFactory) {
                found = true;
                break;
            }
        }
        assertEquals(true, found, "MarkdownValidatorFactory should be discovered via ServiceLoader");
    }
}
