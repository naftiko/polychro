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
import java.util.Set;

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

    @Test
    void buildShouldSilentlySkipAutoDiscoveredFactoryThatRequiresMissingConfig() {
        // Reproduces the second half of issue #20: when validators are auto-discovered
        // (no explicit `validators:` list), a factory that throws
        // IllegalArgumentException on empty config (e.g. JsonSchemaValidatorFactory
        // when neither schemaPath nor schemaNode is provided) must be silently
        // skipped instead of crashing the linter for flag combinations the user
        // did not ask for.
        ValidatorFactory good = new StubValidatorFactory("wellformedness");
        ValidatorFactory needsConfig = new RequiresConfigFactory("ruleset");

        LinterConfig config = new LinterConfig(List.of(), Map.of(), false, "json-schema");
        Linter linter = Linter.builder()
                .config(config)
                .factories(List.of(good, needsConfig))
                .build();

        assertEquals(1, linter.validators().size());
        assertEquals("wellformedness", linter.validators().get(0).name());
    }

    @Test
    void buildShouldStillInstantiateAutoDiscoveredFactoryWhenItsConfigIsProvided() {
        // When the user provided a config block for a factory (e.g. --ruleset foo.yml),
        // that factory must run — only factories without any user-supplied config are
        // silently skipped on IAE.
        ValidatorFactory good = new StubValidatorFactory("wellformedness");
        ValidatorFactory needsConfig = new RequiresConfigFactory("ruleset");

        LinterConfig config = new LinterConfig(
                List.of(),
                Map.of("ruleset", Map.of("rulesetPath", "foo.yml")),
                false,
                "json-schema");
        Linter linter = Linter.builder()
                .config(config)
                .factories(List.of(good, needsConfig))
                .build();

        assertEquals(2, linter.validators().size());
    }

    @Test
    void buildShouldPropagateIllegalArgumentExceptionWhenValidatorWasExplicitlyRequested() {
        // If the user explicitly listed a validator name in `validators:`, a missing-
        // config IAE thrown by its factory is a real misconfiguration and must
        // surface to the caller rather than being silently swallowed.
        ValidatorFactory needsConfig = new RequiresConfigFactory("ruleset");

        LinterConfig config = new LinterConfig(List.of("ruleset"), Map.of(), false, "json-schema");
        Linter.Builder builder = Linter.builder()
                .config(config)
                .factories(List.of(needsConfig));

        assertThrows(IllegalArgumentException.class, builder::build);
    }

    @Test
    void buildShouldPropagateIllegalArgumentExceptionWhenConfigBlockIsProvidedButInvalid() {
        // The user provided a config block (so they explicitly want the validator
        // to run) but the configuration is invalid. The factory's IAE must surface
        // because silently skipping would hide real user errors.
        ValidatorFactory needsConfig = new RequiresConfigFactory("ruleset");

        LinterConfig config = new LinterConfig(
                List.of(),
                Map.of("ruleset", Map.of("wrongKey", "x")),
                false,
                "json-schema");
        Linter.Builder builder = Linter.builder()
                .config(config)
                .factories(List.of(needsConfig));

        assertThrows(IllegalArgumentException.class, builder::build);
    }

    @Test
    void buildShouldWrapAutoDiscoveredValidatorInFormatGateWhenFactoryDeclaresFormats() {
        // Issue #20: when an auto-discovered factory declares supportedFormats(),
        // the resulting validator must be wrapped in a FormatGatedValidator so it
        // only runs against documents whose format is in the supported set. This
        // prevents e.g. the markdown validator from emitting `no-content` errors
        // on YAML documents during auto-discovery.
        ValidatorFactory markdownFactory = new FormatScopedValidatorFactory("markdown", Set.of("markdown"));

        LinterConfig config = new LinterConfig(List.of(), Map.of(), false, "json-schema");
        Linter linter = Linter.builder()
                .config(config)
                .factories(List.of(markdownFactory))
                .build();

        assertEquals(1, linter.validators().size());
        Validator v = linter.validators().get(0);
        assertInstanceOf(FormatGatedValidator.class, v,
                "auto-discovered validator with non-empty supportedFormats must be wrapped");
        FormatGatedValidator gated = (FormatGatedValidator) v;
        assertEquals(Set.of("markdown"), gated.supportedFormats());
    }

    @Test
    void buildShouldNotWrapValidatorWhenFactoryDeclaresEmptySupportedFormats() {
        // Factories that declare an empty supportedFormats() set are treated as
        // unconstrained — wrapping them would be a no-op and adds an extra
        // delegation hop. Verify the builder leaves them bare.
        ValidatorFactory unconstrained = new StubValidatorFactory("wellformedness");

        LinterConfig config = new LinterConfig(List.of(), Map.of(), false, "json-schema");
        Linter linter = Linter.builder()
                .config(config)
                .factories(List.of(unconstrained))
                .build();

        assertEquals(1, linter.validators().size());
        assertFalse(linter.validators().get(0) instanceof FormatGatedValidator,
                "validator with empty supportedFormats must not be wrapped");
    }

    @Test
    void buildShouldNotWrapValidatorWhenFactoryReturnsNullSupportedFormats() {
        // Defensive: although the SPI default returns Set.of(), a third-party
        // ValidatorFactory could return null from supportedFormats(). The builder
        // must treat that the same as an empty set and skip the format gate
        // instead of NPE-ing during auto-discovery.
        ValidatorFactory nullFormats = new FormatScopedValidatorFactory("legacy", null);

        LinterConfig config = new LinterConfig(List.of(), Map.of(), false, "json-schema");
        Linter linter = Linter.builder()
                .config(config)
                .factories(List.of(nullFormats))
                .build();

        assertEquals(1, linter.validators().size());
        assertFalse(linter.validators().get(0) instanceof FormatGatedValidator,
                "validator with null supportedFormats must not be wrapped");
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

    /**
     * Mimics {@link io.polychro.spi.ValidatorFactory} implementations that require a
     * specific configuration key and throw {@link IllegalArgumentException} when it
     * is absent (e.g. {@code JsonSchemaValidatorFactory}, {@code RulesetValidatorFactory}).
     */
    private static class RequiresConfigFactory implements ValidatorFactory {
        private final String name;

        RequiresConfigFactory(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Validator create(ValidatorConfig config) {
            if (config.get("rulesetPath", String.class).isEmpty()) {
                throw new IllegalArgumentException(name + " requires 'rulesetPath'");
            }
            return new StubValidator(name);
        }
    }

    /**
     * Factory that declares a non-empty supported-format set, exercising the
     * {@link Linter.Builder} format-gate wrapping path added for issue #20.
     */
    private static class FormatScopedValidatorFactory implements ValidatorFactory {
        private final String name;
        private final java.util.Set<String> formats;

        FormatScopedValidatorFactory(String name, java.util.Set<String> formats) {
            this.name = name;
            this.formats = formats;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public java.util.Set<String> supportedFormats() {
            return formats;
        }

        @Override
        public Validator create(ValidatorConfig config) {
            return new StubValidator(name);
        }
    }
}
