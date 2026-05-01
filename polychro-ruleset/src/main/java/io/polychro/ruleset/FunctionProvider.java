package io.polychro.ruleset;

import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

/**
 * SPI for providing custom rule functions.
 * Implementations are discovered via {@link ServiceLoader}.
 */
public interface FunctionProvider {

    /**
     * @return the list of rule functions provided by this implementation
     */
    List<RuleFunction> functions();
}
