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
package io.polychro.checkov;

import io.polychro.spi.Validator;
import io.polychro.spi.ValidatorConfig;
import io.polychro.spi.ValidatorFactory;

import java.util.List;

/**
 * Factory for creating {@link CheckovValidator} instances.
 */
public class CheckovValidatorFactory implements ValidatorFactory {

    @Override
    public String name() {
        return CheckovValidator.NAME;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Validator create(ValidatorConfig config) {
        String checkovPath = config.get("checkovPath", String.class).orElse("checkov");
        long timeout = config.get("timeout", Integer.class)
                .map(Integer::longValue)
                .orElse(CheckovValidator.DEFAULT_TIMEOUT_SECONDS);
        List<String> skipChecks = config.get("skipChecks", List.class).orElse(List.of());
        String customCheckDir = config.get("customCheckDir", String.class).orElse(null);
        String framework = config.get("framework", String.class).orElse(null);

        CheckovRunner runner = new CheckovRunner(checkovPath, timeout, skipChecks, customCheckDir);
        return new CheckovValidator(runner, framework);
    }
}
