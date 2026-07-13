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
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.*;

class CheckovValidatorFactoryTest {

    private final CheckovValidatorFactory factory = new CheckovValidatorFactory();

    @Test
    void nameShouldReturnCheckov() {
        assertEquals("checkov", factory.name());
    }

    @Test
    void createShouldReturnValidatorWithDefaults() {
        ValidatorConfig config = new ValidatorConfig(Map.of());
        Validator validator = factory.create(config);
        assertNotNull(validator);
        assertEquals("checkov", validator.name());
    }

    @Test
    void createShouldRespectCustomCheckovPath() {
        ValidatorConfig config = new ValidatorConfig(Map.of("checkovPath", "/opt/checkov"));
        Validator validator = factory.create(config);
        assertNotNull(validator);
        assertInstanceOf(CheckovValidator.class, validator);
    }

    @Test
    void createShouldRespectTimeoutConfig() {
        ValidatorConfig config = new ValidatorConfig(Map.of("timeout", 120));
        Validator validator = factory.create(config);
        assertNotNull(validator);
    }

    @Test
    void createShouldRespectSkipChecksConfig() {
        ValidatorConfig config = new ValidatorConfig(
                Map.of("skipChecks", List.of("CKV_001", "CKV_002")));
        Validator validator = factory.create(config);
        assertNotNull(validator);
    }

    @Test
    void createShouldRespectCustomCheckDirConfig() {
        ValidatorConfig config = new ValidatorConfig(
                Map.of("customCheckDir", "/path/to/checks"));
        Validator validator = factory.create(config);
        assertNotNull(validator);
    }

    @Test
    void createShouldRespectFrameworkOverrideConfig() {
        ValidatorConfig config = new ValidatorConfig(
                Map.of("framework", "terraform"));
        Validator validator = factory.create(config);
        assertNotNull(validator);
    }

    @Test
    void createShouldHandleAllConfigOptions() {
        ValidatorConfig config = new ValidatorConfig(Map.of(
                "checkovPath", "/usr/bin/checkov",
                "timeout", 30,
                "skipChecks", List.of("CKV_001"),
                "customCheckDir", "/custom",
                "framework", "kubernetes"
        ));
        Validator validator = factory.create(config);
        assertNotNull(validator);
        assertEquals("checkov", validator.name());
    }

    @Test
    void serviceLoaderShouldDiscoverFactory() {
        ServiceLoader<ValidatorFactory> loader = ServiceLoader.load(ValidatorFactory.class);
        boolean found = false;
        for (ValidatorFactory f : loader) {
            if ("checkov".equals(f.name())) {
                found = true;
                break;
            }
        }
        assertTrue(found, "ServiceLoader should discover CheckovValidatorFactory");
    }
}
