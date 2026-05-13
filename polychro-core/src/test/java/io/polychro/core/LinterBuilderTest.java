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
package io.polychro.core;

import io.polychro.spi.Diagnostic;
import io.polychro.spi.Document;
import io.polychro.spi.Validator;
import io.polychro.spi.ValidatorConfig;
import io.polychro.spi.ValidatorFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LinterBuilderTest {

    @TempDir
    Path tempDir;

    @Test
    void buildShouldUseDefaultConfigWhenNoneProvided() {
        Linter linter = Linter.builder().build();
        assertNotNull(linter);
    }

    @Test
    void buildShouldAcceptLinterConfigDirectly() {
        LinterConfig config = new LinterConfig(List.of(), Map.of(), true, "json-schema");
        Linter linter = Linter.builder().config(config).build();
        assertNotNull(linter);
    }

    @Test
    void buildShouldLoadConfigFromPath() throws Exception {
        String yaml = """
                failFast: true
                """;
        Path file = tempDir.resolve(".polychro.yml");
        Files.writeString(file, yaml);

        Linter linter = Linter.builder().config(file).build();
        assertNotNull(linter);
    }

    @Test
    void buildShouldLoadConfigFromInputStream() {
        String yaml = """
                failFast: false
                """;
        ByteArrayInputStream is = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
        Linter linter = Linter.builder().config(is).build();
        assertNotNull(linter);
    }

    @Test
    void buildShouldIncludeAdditionalValidators() {
        Validator extra = new StubValidator("extra");
        Linter linter = Linter.builder().addValidator(extra).build();
        assertTrue(linter.validators().contains(extra));
    }

    @Test
    void buildShouldFilterValidatorsByConfigList() {
        ValidatorFactory f1 = new StubValidatorFactory("v1");
        ValidatorFactory f2 = new StubValidatorFactory("v2");

        LinterConfig config = new LinterConfig(List.of("v1"), Map.of(), false, "json-schema");
        Linter linter = Linter.builder()
                .config(config)
                .factories(List.of(f1, f2))
                .build();

        assertEquals(1, linter.validators().size());
        assertEquals("v1", linter.validators().get(0).name());
    }

    @Test
    void buildShouldUseAllFactoriesWhenConfigHasEmptyValidatorList() {
        ValidatorFactory f1 = new StubValidatorFactory("v1");
        ValidatorFactory f2 = new StubValidatorFactory("v2");

        LinterConfig config = new LinterConfig(List.of(), Map.of(), false, "json-schema");
        Linter linter = Linter.builder()
                .config(config)
                .factories(List.of(f1, f2))
                .build();

        assertEquals(2, linter.validators().size());
    }

    @Test
    void buildShouldUseSchemaModelWhenSchemaConfigIsPresent() {
        ValidatorFactory wellformedness = new StubValidatorFactory("wellformedness");
        ValidatorFactory jsonSchema = new StubValidatorFactory("json-schema");
        ValidatorFactory jsonStructure = new StubValidatorFactory("json-structure");
        ValidatorFactory ruleset = new StubValidatorFactory("ruleset");

        LinterConfig config = new LinterConfig(
                List.of(),
                Map.of("json-schema", Map.of("schemaPath", "schema.json")),
                false,
                "json-schema"
        );
        Linter linter = Linter.builder()
                .config(config)
                .factories(List.of(wellformedness, jsonSchema, jsonStructure, ruleset))
                .build();

        assertEquals(3, linter.validators().size());
        assertEquals("wellformedness", linter.validators().get(0).name());
        assertEquals("schema-model", linter.validators().get(1).name());
        assertEquals("ruleset", linter.validators().get(2).name());
    }

        @Test
        void buildShouldUseSchemaModelWhenExplicitlyRequested() {
        ValidatorFactory jsonSchema = new StubValidatorFactory("json-schema");

        LinterConfig config = new LinterConfig(
            List.of("schema-model"),
            Map.of(),
            false,
            "json-schema"
        );

        Linter linter = Linter.builder()
            .config(config)
            .factories(List.of(jsonSchema))
            .build();

        assertEquals(1, linter.validators().size());
        assertEquals("schema-model", linter.validators().get(0).name());
        }

        @Test
        void buildShouldSkipExplicitSchemaModelWhenNoSchemaFactoriesAreAvailable() {
        LinterConfig config = new LinterConfig(
            List.of("schema-model", "known"),
            Map.of(),
            false,
            "json-schema"
        );

        Linter linter = Linter.builder()
            .config(config)
            .factories(List.of(new StubValidatorFactory("known")))
            .build();

        assertEquals(1, linter.validators().size());
        assertEquals("known", linter.validators().get(0).name());
        }

        @Test
        void buildShouldUseSchemaModelWhenOnlyJsonStructureConfigIsPresent() {
        ValidatorFactory wellformedness = new StubValidatorFactory("wellformedness");
        ValidatorFactory jsonStructure = new StubValidatorFactory("json-structure");

        LinterConfig config = new LinterConfig(
            List.of(),
            Map.of("json-structure", Map.of("schemaPath", "structure-schema.json")),
            false,
            "json-structure"
        );

        Linter linter = Linter.builder()
            .config(config)
            .factories(List.of(wellformedness, jsonStructure))
            .build();

        assertEquals(2, linter.validators().size());
        assertEquals("wellformedness", linter.validators().get(0).name());
        assertEquals("schema-model", linter.validators().get(1).name());
        }

    @Test
    void buildShouldPassConfigToFactory() {
        CapturingValidatorFactory factory = new CapturingValidatorFactory("test");

        Map<String, Object> props = Map.of("key", "value");
        LinterConfig config = new LinterConfig(List.of("test"), Map.of("test", props), false, "json-schema");

        Linter.builder()
                .config(config)
                .factories(List.of(factory))
                .build();

        assertNotNull(factory.lastConfig);
        assertEquals("value", factory.lastConfig.properties().get("key"));
    }

    @Test
    void buildShouldSkipUnknownValidatorNames() {
        ValidatorFactory f1 = new StubValidatorFactory("known");

        LinterConfig config = new LinterConfig(List.of("known", "unknown"), Map.of(), false, "json-schema");
        Linter linter = Linter.builder()
                .config(config)
                .factories(List.of(f1))
                .build();

        assertEquals(1, linter.validators().size());
        assertEquals("known", linter.validators().get(0).name());
    }

    private static class StubValidator implements Validator {
        private final String name;

        StubValidator(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public List<Diagnostic> validate(Document doc) {
            return List.of();
        }
    }

    private static class StubValidatorFactory implements ValidatorFactory {
        private final String name;

        StubValidatorFactory(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Validator create(ValidatorConfig config) {
            return new StubValidator(name);
        }
    }

    private static class CapturingValidatorFactory implements ValidatorFactory {
        private final String name;
        ValidatorConfig lastConfig;

        CapturingValidatorFactory(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Validator create(ValidatorConfig config) {
            this.lastConfig = config;
            return new StubValidator(name);
        }
    }
}
