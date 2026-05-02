package io.polychro.mcp;

import io.polychro.core.LinterConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PolychroMcpServerTest {

    @Test
    void buildShouldCreateServerWithDefaultConfig() {
        PolychroMcpServer server = PolychroMcpServer.builder().build();

        assertNotNull(server);
        assertNotNull(server.engine());
    }

    @Test
    void startShouldDelegateToEngine() {
        PolychroMcpServer server = PolychroMcpServer.builder().build();

        // start() delegates to engine.start() which requires full runtime;
        // verify it propagates the underlying error
        assertThrows(Exception.class, server::start);
    }

    @Test
    void stopShouldDelegateToEngine() throws Exception {
        PolychroMcpServer server = PolychroMcpServer.builder().build();

        // stop() on an engine that was never started should not throw
        server.stop();
    }

    @Test
    void buildShouldCreateServerWithExplicitConfig() {
        LinterConfig config = new LinterConfig(List.of(), Map.of(), false, "json-schema");

        PolychroMcpServer server = PolychroMcpServer.builder()
                .config(config)
                .build();

        assertNotNull(server);
    }

    @Test
    void buildShouldCreateServerWithConfigPath(@TempDir Path tempDir) throws Exception {
        Path configFile = tempDir.resolve(".polychro.yml");
        Files.writeString(configFile, "validators: []\nfailFast: false\n");

        PolychroMcpServer server = PolychroMcpServer.builder()
                .config(configFile)
                .build();

        assertNotNull(server);
    }

    @Test
    void buildShouldRegisterExplanations() {
        PolychroMcpServer server = PolychroMcpServer.builder()
                .explanation("rule-a", "Rule A fires when...", "Fix by doing X")
                .explanation("rule-b", "Rule B fires when...", "Fix by doing Y")
                .build();

        assertNotNull(server);
        assertTrue(server.engine().getRegistry().has("do-explain"));
    }

    @Test
    void buildShouldRegisterAllHandlers() {
        PolychroMcpServer server = PolychroMcpServer.builder().build();

        assertTrue(server.engine().getRegistry().has("do-lint"));
        assertTrue(server.engine().getRegistry().has("do-validate-schema"));
        assertTrue(server.engine().getRegistry().has("do-list-rules"));
        assertTrue(server.engine().getRegistry().has("do-explain"));
    }

    @Test
    void resolveConfigShouldReturnDefaultsWhenNothingSet() {
        PolychroMcpServer.Builder builder = PolychroMcpServer.builder();
        LinterConfig config = builder.resolveConfig();

        assertNotNull(config);
        assertTrue(config.validators().isEmpty());
        assertFalse(config.failFast());
    }

    @Test
    void resolveConfigShouldPreferExplicitConfig() {
        LinterConfig explicit = new LinterConfig(
                List.of("wellformedness"), Map.of(), true, "json-schema");

        PolychroMcpServer.Builder builder = PolychroMcpServer.builder().config(explicit);
        LinterConfig result = builder.resolveConfig();

        assertEquals(List.of("wellformedness"), result.validators());
        assertTrue(result.failFast());
    }

    @Test
    void resolveConfigShouldLoadFromPath(@TempDir Path tempDir) throws Exception {
        Path configFile = tempDir.resolve(".polychro.yml");
        Files.writeString(configFile, "validators:\n  - ruleset\nfailFast: true\n");

        PolychroMcpServer.Builder builder = PolychroMcpServer.builder().config(configFile);
        LinterConfig result = builder.resolveConfig();

        assertEquals(List.of("ruleset"), result.validators());
        assertTrue(result.failFast());
    }

    @Test
    void configPathShouldOverridePreviousExplicitConfig() {
        LinterConfig explicit = new LinterConfig(
                List.of("wellformedness"), Map.of(), false, "json-schema");

        PolychroMcpServer.Builder builder = PolychroMcpServer.builder()
                .config(explicit)
                .config(Path.of("nonexistent.yml"));

        // configPath takes precedence — resolveConfig would try to load from path
        // But since we just test the setter logic, verify it doesn't throw on build
        // We test that resolveConfig prefers path when both were called by checking
        // that explicit config is cleared (resolveConfig will try file)
        assertThrows(Exception.class, builder::resolveConfig);
    }

    @Test
    void explicitConfigShouldOverridePreviousPath() {
        LinterConfig explicit = new LinterConfig(List.of(), Map.of(), true, "json-schema");

        PolychroMcpServer.Builder builder = PolychroMcpServer.builder()
                .config(Path.of("nonexistent.yml"))
                .config(explicit);

        LinterConfig result = builder.resolveConfig();
        assertTrue(result.failFast());
    }
}
