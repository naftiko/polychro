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
 */package io.polychro.cli;

import io.polychro.capability.PolychroCapability;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The {@code serve} subcommand — starts an MCP server exposing Polychro linting tools.
 */
@CommandLine.Command(
        name = "serve",
        mixinStandardHelpOptions = true,
        description = "Start an MCP server exposing Polychro linting tools"
)
public class ServeCommand implements Runnable {

    @CommandLine.Option(
            names = {"--config", "-c"},
            description = "Path to .polychro.yml config file"
    )
    Path config;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        int exitCode = execute();
        throw new ExitException(exitCode);
    }

    int execute() {
        var err = spec.commandLine().getErr();

        if (config != null && !Files.exists(config)) {
            err.println("Error: config file not found: " + config);
            return 2;
        }

        PolychroCapability capability = buildCapability();
        try {
            capability.start();
            return 0;
        } catch (Exception e) {
            err.println("Error: MCP server failed: " + e.getMessage());
            return 2;
        }
    }

    PolychroCapability buildCapability() {
        PolychroCapability.Builder builder = PolychroCapability.builder();
        if (config != null) {
            builder.config(config);
        }
        return builder.build();
    }
}
