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

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The {@code serve} subcommand — starts an MCP server exposing Polychro linting tools.
 * <p>
 * {@link PolychroCapability#start()} does not block, so {@link #execute()} waits until shutdown is
 * requested, either by the JVM shutdown hook it registers (Ctrl+C, SIGTERM) or by
 * {@link #requestShutdown()}. Returning earlier would let {@code PolychroCli.main} call
 * {@code System.exit} and tear the server down right after startup.
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

    private final CountDownLatch shutdownRequested = new CountDownLatch(1);
    private final AtomicBoolean stopped = new AtomicBoolean();
    private volatile PolychroCapability running;

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
        } catch (Exception e) {
            err.println("Error: MCP server failed: " + e.getMessage());
            return 2;
        }
        running = capability;

        Thread hook = new Thread(this::requestShutdown, "polychro-serve-shutdown");
        Runtime.getRuntime().addShutdownHook(hook);
        try {
            shutdownRequested.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // Idempotent: also covers a shutdown requested before `running` was set, or an interrupt.
        requestShutdown();
        removeShutdownHook(hook);
        return 0;
    }

    /**
     * Stops the running server (once) and releases {@link #execute()}. Called by the shutdown
     * hook, and directly by tests.
     */
    void requestShutdown() {
        PolychroCapability capability = running;
        if (capability != null && stopped.compareAndSet(false, true)) {
            try {
                capability.stop();
            } catch (Exception e) {
                PrintWriter err = spec.commandLine().getErr();
                err.println("Error: MCP server failed to stop: " + e.getMessage());
                err.flush();
            }
        }
        shutdownRequested.countDown();
    }

    private static void removeShutdownHook(Thread hook) {
        try {
            Runtime.getRuntime().removeShutdownHook(hook);
        } catch (IllegalStateException e) {
            // The JVM is already shutting down: the hook is running or has run.
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
