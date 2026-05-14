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
package io.polychro.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.polychro.core.JsonFormatter;
import io.polychro.core.SarifFormatter;
import io.polychro.core.TextFormatter;
import io.polychro.core.LinterConfig;
import io.polychro.spi.Diagnostic;
import io.polychro.spi.Severity;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LintCommandTest {

    @TempDir
    Path tempDir;

    @Test
    void lintShouldReturnZeroForCleanFile() throws Exception {
        Path file = createFile("clean.yml", "name: test\nversion: \"1.0\"\n");
        int exitCode = executeLint(file.toString());
        assertEquals(0, exitCode);
    }

    @Test
    void lintShouldReturnTwoForFileNotFound() {
        int exitCode = executeLint("nonexistent.yml");
        assertEquals(2, exitCode);
    }

    @Test
    void lintShouldReturnZeroForValidJsonFile() throws Exception {
        Path file = createFile("test.json", "{\"name\": \"test\"}");
        int exitCode = executeLint(file.toString());
        assertEquals(0, exitCode);
    }

    @Test
    void lintShouldReturnZeroForValidXmlFile() throws Exception {
        Path file = createFile("test.xml", "<root><name>test</name></root>");
        int exitCode = executeLint(file.toString());
        assertEquals(0, exitCode);
    }

    @Test
    void lintShouldSupportTextFormat() throws Exception {
        Path file = createFile("test.yml", "name: test\n");
        int exitCode = executeLint("--format", "text", file.toString());
        assertEquals(0, exitCode);
    }

    @Test
    void lintShouldSupportJsonFormat() throws Exception {
        Path file = createFile("test.yml", "name: test\n");
        int exitCode = executeLint("--format", "json", file.toString());
        assertEquals(0, exitCode);
    }

    @Test
    void lintShouldSupportSarifFormat() throws Exception {
        Path file = createFile("test.yml", "name: test\n");
        int exitCode = executeLint("--format", "sarif", file.toString());
        assertEquals(0, exitCode);
    }

    @Test
    void lintShouldSupportValidatorsFlag() throws Exception {
        Path file = createFile("test.yml", "name: test\n");
        int exitCode = executeLint("--validators", "wellformedness", file.toString());
        assertEquals(0, exitCode);
    }

    @Test
    void lintShouldSupportMultipleFiles() throws Exception {
        Path file1 = createFile("a.yml", "name: a\n");
        Path file2 = createFile("b.yml", "name: b\n");
        int exitCode = executeLint(file1.toString(), file2.toString());
        assertEquals(0, exitCode);
    }

    @Test
    void lintShouldSupportConfigFlag() throws Exception {
        Path configFile = createFile(".polychro.yml", "failFast: false\n");
        Path file = createFile("test.yml", "name: test\n");
        int exitCode = executeLint("--config", configFile.toString(), file.toString());
        assertEquals(0, exitCode);
    }

    @Test
    void lintShouldReturnTwoForMissingConfigFile() throws Exception {
        Path file = createFile("test.yml", "name: test\n");
        int exitCode = executeLint("--config", tempDir.resolve("missing.yml").toString(), file.toString());
        assertEquals(2, exitCode);
    }

    @Test
    void lintShouldSupportRulesetFlag() throws Exception {
        Path file = createFile("test.yml", "name: test\n");
        int exitCode = executeLint("--ruleset", "custom-rules.yml", file.toString());
        assertEquals(0, exitCode);
    }

    @Test
    void lintShouldSupportSchemaFlag() throws Exception {
        Path file = createFile("test.yml", "name: test\n");
        int exitCode = executeLint("--schema", "custom-schema.json", file.toString());
        assertEquals(0, exitCode);
    }

    @Test
    void buildConfigFromFlagsShouldConfigureBothSchemaModels() {
        LintCommand command = new LintCommand();
        command.schema = Path.of("custom-schema.json");

        LinterConfig config = command.buildConfigFromFlags();

        assertEquals("custom-schema.json", config.validatorConfigs().get("json-schema").get("schemaPath"));
        assertEquals("custom-schema.json",
                config.validatorConfigs().get("json-structure").get("schemaPath"));
    }

    @Test
    void buildConfigFromFlagsShouldDefaultToJsonSchemaWhenFlagNotProvided() {
        LintCommand command = new LintCommand();

        LinterConfig config = command.buildConfigFromFlags();

        assertEquals("json-schema", config.defaultSchemaValidator());
    }

    @Test
    void buildConfigFromFlagsShouldRespectDefaultSchemaValidatorFlag() {
        LintCommand command = new LintCommand();
        command.defaultSchemaValidator = "json-structure";

        LinterConfig config = command.buildConfigFromFlags();

        assertEquals("json-structure", config.defaultSchemaValidator());
    }

    @Test
    void buildConfigFromFlagsShouldFallBackToJsonSchemaWhenFlagIsBlank() {
        LintCommand command = new LintCommand();
        command.defaultSchemaValidator = "  ";

        LinterConfig config = command.buildConfigFromFlags();

        assertEquals("json-schema", config.defaultSchemaValidator());
    }

    @Test
    void lintShouldReturnTwoForUnparsableFile() throws Exception {
        Path file = createFile("bad.json", "{invalid json content!!!");
        int exitCode = executeLint(file.toString());
        assertEquals(2, exitCode);
    }

    @Test
    void computeExitCodeShouldReturnZeroForEmpty() {
        assertEquals(0, LintCommand.computeExitCode(List.of()));
    }

    @Test
    void computeExitCodeShouldReturnOneForWarningsOnly() {
        Diagnostic warn = new Diagnostic(Severity.WARN, "c", "msg", null, null);
        assertEquals(1, LintCommand.computeExitCode(List.of(warn)));
    }

    @Test
    void computeExitCodeShouldReturnTwoForErrors() {
        Diagnostic error = new Diagnostic(Severity.ERROR, "c", "msg", null, null);
        assertEquals(2, LintCommand.computeExitCode(List.of(error)));
    }

    @Test
    void resolveFormatterShouldReturnTextByDefault() {
        assertInstanceOf(TextFormatter.class, LintCommand.resolveFormatter("text"));
    }

    @Test
    void resolveFormatterShouldReturnJson() {
        assertInstanceOf(JsonFormatter.class, LintCommand.resolveFormatter("json"));
    }

    @Test
    void resolveFormatterShouldReturnSarif() {
        assertInstanceOf(SarifFormatter.class, LintCommand.resolveFormatter("sarif"));
    }

    @Test
    void resolveFormatterShouldReturnTextForUnknown() {
        assertInstanceOf(TextFormatter.class, LintCommand.resolveFormatter("unknown"));
    }

    private int executeLint(String... args) {
        String[] fullArgs = new String[args.length + 1];
        fullArgs[0] = "lint";
        System.arraycopy(args, 0, fullArgs, 1, args.length);
        return PolychroCli.execute(fullArgs);
    }

    private Path createFile(String name, String content) throws Exception {
        Path file = tempDir.resolve(name);
        Files.writeString(file, content);
        return file;
    }
}
