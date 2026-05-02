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
