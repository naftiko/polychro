package io.polychro.cli;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExitExceptionTest {

    @Test
    void exitCodeShouldReturnConstructorValue() {
        ExitException ex = new ExitException(1);
        assertEquals(1, ex.exitCode());
    }

    @Test
    void exitCodeZeroShouldWork() {
        ExitException ex = new ExitException(0);
        assertEquals(0, ex.exitCode());
    }

    @Test
    void messageShouldContainExitCode() {
        ExitException ex = new ExitException(2);
        assertTrue(ex.getMessage().contains("2"));
    }
}
