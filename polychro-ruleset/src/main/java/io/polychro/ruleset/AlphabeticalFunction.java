package io.polychro.ruleset;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Alphabetical function — asserts that array elements or object keys are sorted.
 * <p>
 * Options:
 * <ul>
 *     <li>{@code keyedBy} — for arrays of objects, the field name to sort by</li>
 * </ul>
 */
class AlphabeticalFunction implements RuleFunction {

    @Override
    public String name() {
        return "alphabetical";
    }

    @Override
    public List<String> evaluate(JsonNode targetNode, Map<String, Object> options) {
        if (targetNode == null || targetNode.isNull() || targetNode.isMissingNode()) {
            return List.of("Value must not be null for alphabetical check");
        }

        String keyedBy = options.get("keyedBy") != null ? options.get("keyedBy").toString() : null;

        if (targetNode.isArray()) {
            return checkArray(targetNode, keyedBy);
        }

        if (targetNode.isObject()) {
            return checkObjectKeys(targetNode);
        }

        return List.of("Value must be an array or object for alphabetical check");
    }

    private List<String> checkArray(JsonNode arrayNode, String keyedBy) {
        if (arrayNode.size() <= 1) {
            return List.of();
        }

        List<String> values = new ArrayList<>();
        for (JsonNode item : arrayNode) {
            if (keyedBy != null) {
                JsonNode field = item.get(keyedBy);
                if (field == null || !field.isTextual()) {
                    return List.of("Array element missing field '" + keyedBy + "'");
                }
                values.add(field.asText());
            } else {
                if (item.isTextual()) {
                    values.add(item.asText());
                } else {
                    values.add(item.toString());
                }
            }
        }

        List<String> sorted = new ArrayList<>(values);
        sorted.sort(Comparator.naturalOrder());

        if (!values.equals(sorted)) {
            return List.of("Values are not in alphabetical order");
        }
        return List.of();
    }

    private List<String> checkObjectKeys(JsonNode objectNode) {
        if (objectNode.size() <= 1) {
            return List.of();
        }

        List<String> keys = new ArrayList<>();
        Iterator<String> fieldNames = objectNode.fieldNames();
        while (fieldNames.hasNext()) {
            keys.add(fieldNames.next());
        }

        List<String> sorted = new ArrayList<>(keys);
        sorted.sort(Comparator.naturalOrder());

        if (!keys.equals(sorted)) {
            return List.of("Object keys are not in alphabetical order");
        }
        return List.of();
    }
}
