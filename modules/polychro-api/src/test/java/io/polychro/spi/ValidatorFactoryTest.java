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

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidatorFactoryTest {

    @Test
    void supportedFormatsShouldDefaultToEmpty() {
        ValidatorFactory factory = new StubValidatorFactory();

        assertTrue(factory.supportedFormats().isEmpty());
    }

    @Test
    void supportedProfilesShouldDefaultToEmpty() {
        ValidatorFactory factory = new StubValidatorFactory();

        assertTrue(factory.supportedProfiles().isEmpty());
    }

    @Test
    void customFactoryShouldExposeDeclaredCapabilities() {
        ValidatorFactory factory = new StubValidatorFactory() {
            @Override
            public Set<String> supportedFormats() {
                return Set.of("markdown", "html");
            }

            @Override
            public Set<String> supportedProfiles() {
                return Set.of("skill", "generic");
            }
        };

        assertEquals(Set.of("markdown", "html"), factory.supportedFormats());
        assertEquals(Set.of("skill", "generic"), factory.supportedProfiles());
    }

    private static class StubValidatorFactory implements ValidatorFactory {

        @Override
        public String name() {
            return "stub";
        }

        @Override
        public Validator create(ValidatorConfig config) {
            return new Validator() {
                @Override
                public String name() {
                    return "stub";
                }

                @Override
                public List<Diagnostic> validate(Document doc) {
                    return List.of();
                }
            };
        }
    }
}
