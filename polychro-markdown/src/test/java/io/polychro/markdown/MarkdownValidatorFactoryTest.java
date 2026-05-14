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
package io.polychro.markdown;

import io.polychro.spi.Validator;
import io.polychro.spi.ValidatorConfig;
import io.polychro.spi.ValidatorFactory;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void supportedFormatsShouldAdvertiseMarkdown() {
        assertEquals(Set.of("markdown"), factory.supportedFormats());
    }

    @Test
    void supportedProfilesShouldAdvertiseMarkdownProfiles() {
        assertEquals(Set.of("generic", "skill", "agents", "instructions"),
                factory.supportedProfiles());
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
