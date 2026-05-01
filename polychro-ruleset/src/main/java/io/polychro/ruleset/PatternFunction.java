package io.polychro.ruleset;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Pattern function — asserts the target string matches (or does not match) a regex.
 * <p>
 * Options:
 * <ul>
 *     <li>{@code match} — regex the value must match</li>
 *     <li>{@code notMatch} — regex the value must NOT match</li>
 * </ul>
 */
class PatternFunction implements RuleFunction {

    @Override
    public String name() {
        return "pattern";
    }

    @Override
    public List<String> evaluate(JsonNode targetNode, Map<String, Object> options) {
        if (targetNode == null || targetNode.isNull() || targetNode.isMissingNode()) {
            return List.of("Value must be a string for pattern matching");
        }
        if (!targetNode.isTextual()) {
            return List.of("Value must be a string for pattern matching");
        }

        String value = targetNode.asText();
        String match = stringOption(options, "match");
        String notMatch = stringOption(options, "notMatch");

        if (match != null) {
            try {
                if (!Pattern.compile(match).matcher(value).find()) {
                    return List.of("Value must match pattern: " + match);
                }
            } catch (PatternSyntaxException e) {
                return List.of("Invalid regex pattern: " + match);
            }
        }

        if (notMatch != null) {
            try {
                if (Pattern.compile(notMatch).matcher(value).find()) {
                    return List.of("Value must not match pattern: " + notMatch);
                }
            } catch (PatternSyntaxException e) {
                return List.of("Invalid regex pattern: " + notMatch);
            }
        }

        return List.of();
    }

    private String stringOption(Map<String, Object> options, String key) {
        Object val = options.get(key);
        return val != null ? val.toString() : null;
    }
}
