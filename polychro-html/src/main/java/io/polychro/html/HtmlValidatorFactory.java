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
package io.polychro.html;

import io.polychro.spi.Validator;
import io.polychro.spi.ValidatorConfig;
import io.polychro.spi.ValidatorFactory;

import java.util.Set;

/**
 * Factory for creating {@link HtmlValidator} instances.
 */
public class HtmlValidatorFactory implements ValidatorFactory {

    private static final Set<String> SUPPORTED_FORMATS = Set.of("html");
    private static final Set<String> SUPPORTED_PROFILES = Set.of(
            "generic", "document", "fragment", "email", "embedded-ui");

    @Override
    public String name() {
        return HtmlValidator.NAME;
    }

    @Override
    public Set<String> supportedFormats() {
        return SUPPORTED_FORMATS;
    }

    @Override
    public Set<String> supportedProfiles() {
        return SUPPORTED_PROFILES;
    }

    @Override
    public Validator create(ValidatorConfig config) {
        String profileName = config.get("profile", String.class).orElse("generic");
        return new HtmlValidator(HtmlProfile.forName(profileName));
    }
}
