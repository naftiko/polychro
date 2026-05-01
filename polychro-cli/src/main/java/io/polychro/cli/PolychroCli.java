package io.polychro.cli;

import picocli.CommandLine;

/**
 * Main entry point for the Polychro CLI.
 */
@CommandLine.Command(
        name = "polychro",
        mixinStandardHelpOptions = true,
        version = "polychro 0.1.0",
        description = "Polyglot spec linting engine",
        subcommands = {LintCommand.class}
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
