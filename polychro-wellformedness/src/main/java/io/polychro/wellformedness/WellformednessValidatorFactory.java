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
