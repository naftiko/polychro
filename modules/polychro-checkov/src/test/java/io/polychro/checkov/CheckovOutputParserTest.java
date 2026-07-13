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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CheckovOutputParserTest {

    private final CheckovOutputParser parser = new CheckovOutputParser();

    @Test
    void parseShouldReturnEmptyListForNull() {
        List<CheckResult> results = parser.parse(null);
        assertTrue(results.isEmpty());
    }

    @Test
    void parseShouldReturnEmptyListForBlank() {
        List<CheckResult> results = parser.parse("  ");
        assertTrue(results.isEmpty());
    }

    @Test
    void parseShouldReturnEmptyListForMalformedJson() {
        List<CheckResult> results = parser.parse("not json at all");
        assertTrue(results.isEmpty());
    }

    @Test
    void parseShouldHandleSingleObjectWithFailedChecks() {
        String json = """
                {
                  "results": {
                    "passed_checks": [],
                    "failed_checks": [
                      {
                        "check_id": "CKV_AWS_001",
                        "check_name": "Ensure S3 bucket encryption",
                        "severity": "HIGH",
                        "file_path": "/s3.tf",
                        "file_line_range": [5, 10],
                        "guideline": "https://docs.example.com/CKV_AWS_001"
                      }
                    ]
                  }
                }
                """;

        List<CheckResult> results = parser.parse(json);
        assertEquals(1, results.size());

        CheckResult result = results.get(0);
        assertEquals("CKV_AWS_001", result.checkId());
        assertEquals("Ensure S3 bucket encryption", result.checkName());
        assertEquals("FAILED", result.result());
        assertEquals("HIGH", result.severity());
        assertEquals("/s3.tf", result.filePath());
        assertEquals(5, result.startLine());
        assertEquals(10, result.endLine());
        assertEquals("https://docs.example.com/CKV_AWS_001", result.guidelineUrl());
    }

    @Test
    void parseShouldHandlePassedChecks() {
        String json = """
                {
                  "results": {
                    "passed_checks": [
                      {
                        "check_id": "CKV_AWS_002",
                        "check_name": "Ensure logging enabled",
                        "severity": "MEDIUM",
                        "file_path": "/main.tf",
                        "file_line_range": [1, 3]
                      }
                    ],
                    "failed_checks": []
                  }
                }
                """;

        List<CheckResult> results = parser.parse(json);
        assertEquals(1, results.size());
        assertEquals("PASSED", results.get(0).result());
    }

    @Test
    void parseShouldHandleArrayOfFrameworkResults() {
        String json = """
                [
                  {
                    "results": {
                      "passed_checks": [],
                      "failed_checks": [
                        {
                          "check_id": "CKV_K8S_001",
                          "check_name": "Container not privileged",
                          "severity": "CRITICAL",
                          "file_path": "/deploy.yaml",
                          "file_line_range": [10, 20]
                        }
                      ]
                    }
                  },
                  {
                    "results": {
                      "passed_checks": [],
                      "failed_checks": [
                        {
                          "check_id": "CKV_K8S_002",
                          "check_name": "No root user",
                          "severity": "HIGH",
                          "file_path": "/pod.yaml",
                          "file_line_range": [5, 8]
                        }
                      ]
                    }
                  }
                ]
                """;

        List<CheckResult> results = parser.parse(json);
        assertEquals(2, results.size());
        assertEquals("CKV_K8S_001", results.get(0).checkId());
        assertEquals("CKV_K8S_002", results.get(1).checkId());
    }

    @Test
    void parseShouldHandleMissingSeverity() {
        String json = """
                {
                  "results": {
                    "passed_checks": [],
                    "failed_checks": [
                      {
                        "check_id": "CKV_001",
                        "check_name": "Some check",
                        "file_path": "/file.yaml",
                        "file_line_range": [1, 2]
                      }
                    ]
                  }
                }
                """;

        List<CheckResult> results = parser.parse(json);
        assertEquals(1, results.size());
        assertNull(results.get(0).severity());
    }

    @Test
    void parseShouldHandleMissingFileLineRange() {
        String json = """
                {
                  "results": {
                    "passed_checks": [],
                    "failed_checks": [
                      {
                        "check_id": "CKV_001",
                        "check_name": "Some check",
                        "severity": "LOW",
                        "file_path": "/file.yaml"
                      }
                    ]
                  }
                }
                """;

        List<CheckResult> results = parser.parse(json);
        assertEquals(1, results.size());
        assertEquals(0, results.get(0).startLine());
        assertEquals(0, results.get(0).endLine());
    }

    @Test
    void parseShouldHandleEmptyResults() {
        String json = """
                {
                  "results": {
                    "passed_checks": [],
                    "failed_checks": []
                  }
                }
                """;

        List<CheckResult> results = parser.parse(json);
        assertTrue(results.isEmpty());
    }

    @Test
    void parseShouldHandleMissingGuideline() {
        String json = """
                {
                  "results": {
                    "passed_checks": [],
                    "failed_checks": [
                      {
                        "check_id": "CKV_001",
                        "check_name": "Check",
                        "severity": "MEDIUM",
                        "file_path": "/file.yaml",
                        "file_line_range": [1, 1]
                      }
                    ]
                  }
                }
                """;

        List<CheckResult> results = parser.parse(json);
        assertEquals(1, results.size());
        assertNull(results.get(0).guidelineUrl());
    }

    @Test
    void parseShouldHandleFileLineRangeWithLessThanTwoElements() {
        String json = """
                {
                  "results": {
                    "passed_checks": [],
                    "failed_checks": [
                      {
                        "check_id": "CKV_001",
                        "check_name": "Check",
                        "severity": "LOW",
                        "file_path": "/file.yaml",
                        "file_line_range": [5]
                      }
                    ]
                  }
                }
                """;

        List<CheckResult> results = parser.parse(json);
        assertEquals(1, results.size());
        assertEquals(0, results.get(0).startLine());
        assertEquals(0, results.get(0).endLine());
    }

    @Test
    void parseShouldHandleNullFieldValues() {
        String json = """
                {
                  "results": {
                    "passed_checks": [],
                    "failed_checks": [
                      {
                        "check_id": null,
                        "check_name": null,
                        "severity": null,
                        "file_path": null,
                        "guideline": null,
                        "file_line_range": [1, 2]
                      }
                    ]
                  }
                }
                """;

        List<CheckResult> results = parser.parse(json);
        assertEquals(1, results.size());
        assertNull(results.get(0).checkId());
        assertNull(results.get(0).checkName());
        assertNull(results.get(0).severity());
        assertNull(results.get(0).filePath());
        assertNull(results.get(0).guidelineUrl());
    }

    @Test
    void parseShouldHandleNonArrayNonObjectRoot() {
        String json = "\"just a string\"";

        List<CheckResult> results = parser.parse(json);
        assertTrue(results.isEmpty());
    }

    @Test
    void parseShouldHandleEmptyArrayRoot() {
        String json = "[]";

        List<CheckResult> results = parser.parse(json);
        assertTrue(results.isEmpty());
    }

    @Test
    void parseShouldHandleNonArrayFailedChecks() {
        String json = """
                {
                  "results": {
                    "passed_checks": "not-array",
                    "failed_checks": "not-array"
                  }
                }
                """;

        List<CheckResult> results = parser.parse(json);
        assertTrue(results.isEmpty());
    }
}
