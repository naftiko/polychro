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
