package io.polychro.markdown;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;

/**
 * Parses YAML frontmatter delimited by --- from Markdown content.
 */
class FrontmatterParser {

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());
    private static final String DELIMITER = "---";

    FrontmatterResult parse(String content) {
        if (content == null || !content.startsWith(DELIMITER)) {
            return new FrontmatterResult(null, content, 1, null);
        }

        int firstDelimiterEnd = content.indexOf('\n');
        if (firstDelimiterEnd == -1) {
            // Only "---" with no newline — no frontmatter
            return new FrontmatterResult(null, content, 1, null);
        }

        String afterFirst = content.substring(firstDelimiterEnd + 1);
        int closingIndex;
        if (afterFirst.startsWith(DELIMITER + "\n") || afterFirst.equals(DELIMITER)) {
            closingIndex = 0;
        } else {
            int nlPos = afterFirst.indexOf("\n" + DELIMITER);
            if (nlPos == -1) {
                return new FrontmatterResult(null, content, 1,
                        "Frontmatter missing closing '---' delimiter");
            }
            closingIndex = nlPos + 1;
        }

        String yamlContent = afterFirst.substring(0, closingIndex == 0 ? 0 : closingIndex - 1);
        int closingLineEnd = afterFirst.indexOf('\n', closingIndex + 1);
        String body;
        int bodyStartLine;

        // Count lines in frontmatter section: 1 for opening ---, plus lines in yaml, plus 1 for closing ---
        int frontmatterLines = 2 + countLines(yamlContent);

        if (closingLineEnd == -1) {
            body = "";
            bodyStartLine = frontmatterLines + 1;
        } else {
            body = afterFirst.substring(closingLineEnd + 1);
            bodyStartLine = frontmatterLines + 1;
        }

        if (yamlContent.isBlank()) {
            return new FrontmatterResult(null, body, bodyStartLine, null);
        }

        try {
            JsonNode data = YAML_MAPPER.readTree(yamlContent);
            if (data.isNull()) {
                return new FrontmatterResult(null, body, bodyStartLine, null);
            }
            if (!data.isObject()) {
                return new FrontmatterResult(null, body, bodyStartLine,
                        "Frontmatter must be a YAML mapping, found: " + data.getNodeType());
            }
            return new FrontmatterResult(data, body, bodyStartLine, null);
        } catch (IOException e) {
            return new FrontmatterResult(null, body, bodyStartLine,
                    "Frontmatter YAML syntax error: " + e.getMessage());
        }
    }

    private int countLines(String text) {
        if (text.isEmpty()) {
            return 0;
        }
        int count = 1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                count++;
            }
        }
        return count;
    }
}
