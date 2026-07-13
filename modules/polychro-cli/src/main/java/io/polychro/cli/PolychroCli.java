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

import picocli.CommandLine;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

/**
 * Main entry point for the Polychro CLI.
 */
@CommandLine.Command(
        name = "polychro",
        mixinStandardHelpOptions = true,
        versionProvider = PolychroCli.VersionProvider.class,
        description = "Polyglot spec linting engine",
        subcommands = {LintCommand.class, ServeCommand.class}
)
public class PolychroCli implements Runnable {

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }

    public static void main(String[] args) {
        System.exit(execute(args));
    }

    static int execute(String[] args) {
        return buildCommandLine().execute(args);
    }

    static CommandLine buildCommandLine() {
        CommandLine cmd = new CommandLine(new PolychroCli());
        cmd.setExecutionExceptionHandler((ex, commandLine, parseResult) -> {
            if (ex instanceof ExitException exitEx) {
                return exitEx.exitCode();
            }
            commandLine.getErr().println("Error: " + ex.getMessage());
            return 2;
        });
        return cmd;
    }

    /**
     * Reads the version from {@code version.properties}, which is generated at
     * build time by Maven resource filtering from {@code ${project.version}}.
     * This ensures the binary always reports the version it was actually built from,
     * rather than a hardcoded string that can drift from the POM.
     */
    static class VersionProvider implements CommandLine.IVersionProvider {
        @Override
        public String[] getVersion() {
            Properties props = new Properties();
            try (InputStream in =
                    VersionProvider.class.getClassLoader().getResourceAsStream("version.properties")) {
                if (in == null) {
                    return new String[]{"polychro (version unknown)"};
                }
                props.load(in);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return new String[]{"polychro " + props.getProperty("version", "unknown")};
        }
    }
}
