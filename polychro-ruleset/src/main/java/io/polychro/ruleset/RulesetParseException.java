package io.polychro.ruleset;

/**
 * Thrown when a ruleset YAML file cannot be parsed into a valid Ruleset.
 */
class RulesetParseException extends RuntimeException {

    RulesetParseException(String message) {
        super(message);
    }
}
