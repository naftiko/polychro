package io.polychro.cli;

/**
 * Exception used to signal a non-zero exit code from a picocli command
 * without calling {@link System#exit(int)} directly (which would prevent testing).
 */
class ExitException extends RuntimeException {

    private final int exitCode;

    ExitException(int exitCode) {
        super("exit " + exitCode);
        this.exitCode = exitCode;
    }

    int exitCode() {
        return exitCode;
    }
}
