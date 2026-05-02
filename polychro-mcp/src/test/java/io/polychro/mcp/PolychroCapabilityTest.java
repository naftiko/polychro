package io.polychro.mcp;

import io.polychro.core.LinterConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PolychroCapabilityTest {

    @Test
    void buildShouldCreateCapabilityWithDefaultConfig() {
        PolychroCapability capability = PolychroCapability.builder().build();

        assertNotNull(capability);
        assertNotNull(capability.getStepHandlerRegistry());
    }

    @Test
    void startShouldDelegate() {
        PolychroCapability capability = PolychroCapability.builder().build();

        // start() requires full runtime (OTel, Restlet); verify it propagates the error
        assertThrows(Throwable.class, capability::start);
    }

    @Test
    void stopShouldDelegate() {
        PolychroCapability capability = PolychroCapability.builder().build();

        // stop() requires full runtime; verify it propagates the error
        assertThrows(Throwable.class, capability::stop);
    }

    @Test
    void buildShouldCreateCapabilityWithExplicitConfig() {
        LinterConfig config = new LinterConfig(List.of(), Map.of(), false, "json-schema");

        PolychroCapability capability = PolychroCapability.builder()
                .config(config)
                .build();

        assertNotNull(capability);
    }

    @Test
    void buildShouldCreateCapabilityWithConfigPath(@TempDir Path tempDir) throws Exception {
        Path configFile = tempDir.resolve(".polychro.yml");
        Files.writeString(configFile, "validators: []\nfailFast: false\n");

        PolychroCapability capability = PolychroCapability.builder()
                .config(configFile)
                .build();

        assertNotNull(capability);
    }

    @Test
    void buildShouldRegisterExplanations() {
        PolychroCapability capability = PolychroCapability.builder()
                .explanation("rule-a", "Rule A fires when...", "Fix by doing X")
                .explanation("rule-b", "Rule B fires when...", "Fix by doing Y")
                .build();

        assertNotNull(capability);
        assertTrue(capability.getStepHandlerRegistry().has("do-explain"));
    }

    @Test
    void buildShouldRegisterAllHandlers() {
        PolychroCapability capability = PolychroCapability.builder().build();

        assertTrue(capability.getStepHandlerRegistry().has("do-lint"));
        assertTrue(capability.getStepHandlerRegistry().has("do-validate-schema"));
        assertTrue(capability.getStepHandlerRegistry().has("do-list-rules"));
        assertTrue(capability.getStepHandlerRegistry().has("do-explain"));
    }

    @Test
    void resolveConfigShouldReturnDefaultsWhenNothingSet() {
        PolychroCapability.Builder builder = PolychroCapability.builder();
        LinterConfig config = builder.resolveConfig();

        assertNotNull(config);
        assertTrue(config.validators().isEmpty());
        assertFalse(config.failFast());
    }

    @Test
    void resolveConfigShouldPreferExplicitConfig() {
        LinterConfig explicit = new LinterConfig(
                List.of("wellformedness"), Map.of(), true, "json-schema");

        PolychroCapability.Builder builder = PolychroCapability.builder().config(explicit);
        LinterConfig result = builder.resolveConfig();

        assertEquals(List.of("wellformedness"), result.validators());
        assertTrue(result.failFast());
    }

    @Test
    void resolveConfigShouldLoadFromPath(@TempDir Path tempDir) throws Exception {
        Path configFile = tempDir.resolve(".polychro.yml");
        Files.writeString(configFile, "validators:\n  - ruleset\nfailFast: true\n");

        PolychroCapability.Builder builder = PolychroCapability.builder().config(configFile);
        LinterConfig result = builder.resolveConfig();

        assertEquals(List.of("ruleset"), result.validators());
        assertTrue(result.failFast());
    }

    @Test
    void configPathShouldOverridePreviousExplicitConfig() {
        LinterConfig explicit = new LinterConfig(
                List.of("wellformedness"), Map.of(), false, "json-schema");

        PolychroCapability.Builder builder = PolychroCapability.builder()
                .config(explicit)
                .config(Path.of("nonexistent.yml"));

        // configPath takes precedence — resolveConfig would try to load from path
        assertThrows(Exception.class, builder::resolveConfig);
    }

    @Test
    void explicitConfigShouldOverridePreviousPath() {
        LinterConfig explicit = new LinterConfig(List.of(), Map.of(), true, "json-schema");

        PolychroCapability.Builder builder = PolychroCapability.builder()
                .config(Path.of("nonexistent.yml"))
                .config(explicit);

        LinterConfig result = builder.resolveConfig();
        assertTrue(result.failFast());
    }
}
