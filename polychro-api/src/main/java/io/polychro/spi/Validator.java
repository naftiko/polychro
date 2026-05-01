package io.polychro.spi;

import java.util.List;

/**
 * A document validator that produces diagnostics.
 * <p>
 * Implementations are discovered via {@link ValidatorFactory} and {@link java.util.ServiceLoader}.
 */
public interface Validator {

    /**
     * @return the unique name of this validator (e.g. "wellformedness", "json-schema", "ruleset")
     */
    String name();

    /**
     * Validate the given document and return any diagnostics found.
     *
     * @param doc the document to validate
     * @return a list of diagnostics, empty if the document is valid
     */
    List<Diagnostic> validate(Document doc);
}
