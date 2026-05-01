package io.polychro.markdown;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Result of parsing YAML frontmatter from a Markdown document.
 */
record FrontmatterResult(JsonNode data, String body, int bodyStartLine, String errorMessage) {

    boolean hasError() {
        return errorMessage != null;
    }

    boolean hasFrontmatter() {
        return data != null && !data.isNull() && !data.isMissingNode();
    }
}
