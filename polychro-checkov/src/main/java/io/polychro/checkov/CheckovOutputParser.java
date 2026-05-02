/**
 * Copyright 2026 Naftiko
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.polychro.checkov;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses Checkov JSON output into CheckResult records.
 */
class CheckovOutputParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    List<CheckResult> parse(String jsonOutput) {
        if (jsonOutput == null || jsonOutput.isBlank()) {
            return List.of();
        }

        try {
            JsonNode root = MAPPER.readTree(jsonOutput);
            return parseRoot(root);
        } catch (IOException e) {
            return List.of();
        }
    }

    List<CheckResult> parseRoot(JsonNode root) {
        List<CheckResult> results = new ArrayList<>();

        if (root.isArray()) {
            for (JsonNode entry : root) {
                results.addAll(parseCheckovEntry(entry));
            }
        } else if (root.isObject()) {
            results.addAll(parseCheckovEntry(root));
        }

        return results;
    }

    private List<CheckResult> parseCheckovEntry(JsonNode entry) {
        List<CheckResult> results = new ArrayList<>();

        JsonNode resultsNode = entry.path("results");
        parseResultArray(resultsNode.path("passed_checks"), "PASSED", results);
        parseResultArray(resultsNode.path("failed_checks"), "FAILED", results);

        return results;
    }

    private void parseResultArray(JsonNode checksArray, String resultStatus, List<CheckResult> results) {
        if (!checksArray.isArray()) {
            return;
        }
        for (JsonNode check : checksArray) {
            results.add(parseCheck(check, resultStatus));
        }
    }

    CheckResult parseCheck(JsonNode check, String resultStatus) {
        String checkId = textOrNull(check, "check_id");
        String checkName = textOrNull(check, "check_name");
        String severity = textOrNull(check, "severity");
        String filePath = textOrNull(check, "file_path");
        String guidelineUrl = textOrNull(check, "guideline");

        int startLine = 0;
        int endLine = 0;
        JsonNode fileLineRange = check.path("file_line_range");
        if (fileLineRange.isArray() && fileLineRange.size() >= 2) {
            startLine = fileLineRange.get(0).asInt(0);
            endLine = fileLineRange.get(1).asInt(0);
        }

        return new CheckResult(checkId, checkName, resultStatus, severity,
                filePath, startLine, endLine, guidelineUrl);
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.asText();
    }
}
