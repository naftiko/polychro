package io.polychro.checkov;

/**
 * Checkov framework identifiers used for the --framework flag.
 */
enum CheckovFramework {
    TERRAFORM("terraform"),
    KUBERNETES("kubernetes"),
    CLOUDFORMATION("cloudformation"),
    DOCKERFILE("dockerfile"),
    YAML("yaml");

    private final String value;

    CheckovFramework(String value) {
        this.value = value;
    }

    String value() {
        return value;
    }
}
