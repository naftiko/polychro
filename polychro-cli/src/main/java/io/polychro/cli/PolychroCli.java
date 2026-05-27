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

/**
 * Main entry point for the Polychro CLI.
 */
@CommandLine.Command(
        name = "polychro",
        mixinStandardHelpOptions = true,
        version = "polychro 1.0.0-alpha3",
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
}
