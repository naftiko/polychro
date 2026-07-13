// Copyright 2026 Naftiko — Apache License 2.0

package polychro

import (
	"encoding/json"
	"errors"
	"os"
	"strings"
	"testing"
)

func TestDiagnosticToAgentFormat(t *testing.T) {
	d := Diagnostic{
		Severity: SeverityError,
		Code:     "no-trailing-slash",
		Message:  "Remove trailing slash from baseUri",
		Path:     "$.consumes[0].baseUri",
	}
	output := d.ToAgentFormat()
	if !strings.Contains(output, "[ERROR] no-trailing-slash") {
		t.Errorf("expected severity and code, got: %s", output)
	}
	if !strings.Contains(output, "Path: $.consumes[0].baseUri") {
		t.Errorf("expected path, got: %s", output)
	}
}

func TestDiagnosticToAgentFormatWithRange(t *testing.T) {
	d := Diagnostic{
		Severity: SeverityWarn,
		Code:     "naming",
		Message:  "Use kebab",
		Path:     "$.info.name",
		Range:    &SourceRange{Start: SourcePosition{Line: 5, Character: 2}, End: SourcePosition{Line: 5, Character: 10}},
	}
	output := d.ToAgentFormat()
	if !strings.Contains(output, "Location: line 5:2") {
		t.Errorf("expected location, got: %s", output)
	}
}

func TestDiagnosticToAgentFormatWithSuggestion(t *testing.T) {
	d := Diagnostic{
		Severity:   SeverityInfo,
		Code:       "tags",
		Message:    "Add tags",
		Path:       "$.info",
		Suggestion: "Add a tags array",
	}
	output := d.ToAgentFormat()
	if !strings.Contains(output, "Fix: Add a tags array") {
		t.Errorf("expected suggestion, got: %s", output)
	}
}

func TestLintResultHasErrors(t *testing.T) {
	result := LintResult{
		Diagnostics: []Diagnostic{
			{Severity: SeverityError, Code: "x", Message: "m", Path: "$"},
		},
	}
	if !result.HasErrors() {
		t.Error("expected HasErrors to be true")
	}
}

func TestLintResultHasErrorsFalse(t *testing.T) {
	result := LintResult{
		Diagnostics: []Diagnostic{
			{Severity: SeverityWarn, Code: "x", Message: "m", Path: "$"},
		},
	}
	if result.HasErrors() {
		t.Error("expected HasErrors to be false for warnings only")
	}
}

func TestLintResultToAgentFormatEmpty(t *testing.T) {
	result := LintResult{}
	output := result.ToAgentFormat()
	if output != "No issues found." {
		t.Errorf("expected 'No issues found.', got: %s", output)
	}
}

func TestLintResultToAgentFormatMultiple(t *testing.T) {
	result := LintResult{
		Diagnostics: []Diagnostic{
			{Severity: SeverityError, Code: "a", Message: "first", Path: "$.x"},
			{Severity: SeverityWarn, Code: "b", Message: "second", Path: "$.y"},
		},
	}
	output := result.ToAgentFormat()
	if !strings.Contains(output, "Total: 2 issue(s)") {
		t.Errorf("expected total count, got: %s", output)
	}
}

func TestParseLintOutputArray(t *testing.T) {
	input := `[{"severity":"error","code":"rule-1","message":"Bad","path":"$.info"}]`
	result, err := parseLintOutput([]byte(input))
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if len(result.Diagnostics) != 1 {
		t.Fatalf("expected 1 diagnostic, got %d", len(result.Diagnostics))
	}
	if result.Diagnostics[0].Code != "rule-1" {
		t.Errorf("expected code 'rule-1', got '%s'", result.Diagnostics[0].Code)
	}
}

func TestParseLintOutputAgentFormat(t *testing.T) {
	input := `{"diagnostics":[{"severity":"warn","code":"tags","message":"Add tags","path":"$.info"}],"tokens":12}`
	result, err := parseLintOutput([]byte(input))
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if len(result.Diagnostics) != 1 {
		t.Fatalf("expected 1 diagnostic, got %d", len(result.Diagnostics))
	}
	if result.Tokens != 12 {
		t.Errorf("expected tokens=12, got %d", result.Tokens)
	}
}

func TestParseLintOutputEmpty(t *testing.T) {
	result, err := parseLintOutput([]byte(""))
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if len(result.Diagnostics) != 0 {
		t.Errorf("expected 0 diagnostics, got %d", len(result.Diagnostics))
	}
}

func TestResolveBinaryNotFound(t *testing.T) {
	// Unset POLYCHRO_BIN and ensure it's not on PATH
	t.Setenv("POLYCHRO_BIN", "")
	t.Setenv("PATH", "")
	_, err := ResolveBinary()
	if !errors.Is(err, BinaryNotFoundError) {
		t.Errorf("expected BinaryNotFoundError, got: %v", err)
	}
}

func TestResolveBinaryFromEnv(t *testing.T) {
	// Create a temp file to simulate the binary
	tmpDir := t.TempDir()
	binaryPath := tmpDir + "/polychro"
	if err := writeTestBinary(binaryPath); err != nil {
		t.Fatalf("failed to create test binary: %v", err)
	}
	t.Setenv("POLYCHRO_BIN", binaryPath)
	path, err := ResolveBinary()
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if path != binaryPath {
		t.Errorf("expected %s, got %s", binaryPath, path)
	}
}

func TestDiagnosticJSON(t *testing.T) {
	d := Diagnostic{
		Severity: SeverityError,
		Code:     "test",
		Message:  "test message",
		Path:     "$.root",
	}
	data, err := json.Marshal(d)
	if err != nil {
		t.Fatalf("marshal failed: %v", err)
	}

	var parsed Diagnostic
	if err := json.Unmarshal(data, &parsed); err != nil {
		t.Fatalf("unmarshal failed: %v", err)
	}
	if parsed.Code != "test" {
		t.Errorf("expected code 'test', got '%s'", parsed.Code)
	}
}

func writeTestBinary(path string) error {
	f, err := createFile(path)
	if err != nil {
		return err
	}
	return f.Close()
}

func createFile(path string) (*file, error) {
	return os.Create(path)
}

type file = os.File
