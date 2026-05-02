// Copyright 2026 Naftiko — Apache License 2.0

// Package polychro provides a thin wrapper around the Polychro native binary
// for deterministic linting of spec-driven YAML/JSON documents.
package polychro

import (
	"context"
	"encoding/json"
	"fmt"
	"os/exec"
	"strings"
	"time"
)

// Severity represents a diagnostic severity level.
type Severity string

const (
	SeverityError Severity = "error"
	SeverityWarn  Severity = "warn"
	SeverityInfo  Severity = "info"
	SeverityHint  Severity = "hint"
)

// SourcePosition represents a position in a document.
type SourcePosition struct {
	Line      int `json:"line"`
	Character int `json:"character"`
}

// SourceRange represents a range in a document.
type SourceRange struct {
	Start SourcePosition `json:"start"`
	End   SourcePosition `json:"end"`
}

// Diagnostic represents a single lint finding.
type Diagnostic struct {
	Severity   Severity     `json:"severity"`
	Code       string       `json:"code"`
	Message    string       `json:"message"`
	Path       string       `json:"path"`
	Range      *SourceRange `json:"range,omitempty"`
	Suggestion string       `json:"suggestion,omitempty"`
}

// ToAgentFormat formats this diagnostic for LLM consumption.
func (d Diagnostic) ToAgentFormat() string {
	var sb strings.Builder
	fmt.Fprintf(&sb, "[%s] %s\n", strings.ToUpper(string(d.Severity)), d.Code)
	fmt.Fprintf(&sb, "  Path: %s\n", d.Path)
	fmt.Fprintf(&sb, "  Message: %s", d.Message)
	if d.Range != nil {
		fmt.Fprintf(&sb, "\n  Location: line %d:%d", d.Range.Start.Line, d.Range.Start.Character)
	}
	if d.Suggestion != "" {
		fmt.Fprintf(&sb, "\n  Fix: %s", d.Suggestion)
	}
	return sb.String()
}

// LintResult represents the outcome of a lint operation.
type LintResult struct {
	Diagnostics []Diagnostic `json:"diagnostics"`
	Tokens      int          `json:"tokens,omitempty"`
}

// HasErrors returns true if any diagnostic has error severity.
func (r LintResult) HasErrors() bool {
	for _, d := range r.Diagnostics {
		if d.Severity == SeverityError {
			return true
		}
	}
	return false
}

// ToAgentFormat formats all diagnostics for LLM consumption.
func (r LintResult) ToAgentFormat() string {
	if len(r.Diagnostics) == 0 {
		return "No issues found."
	}
	var sb strings.Builder
	for i, d := range r.Diagnostics {
		if i > 0 {
			sb.WriteString("\n\n")
		}
		sb.WriteString(d.ToAgentFormat())
	}
	fmt.Fprintf(&sb, "\n\nTotal: %d issue(s)", len(r.Diagnostics))
	return sb.String()
}

// Options configures the Polychro linter.
type Options struct {
	// Ruleset is the path to a custom ruleset file.
	Ruleset string
	// Schema is the path to a custom schema file.
	Schema string
	// Config is the path to a .polychro.yml config file.
	Config string
	// Timeout for the binary execution (default: 30s).
	Timeout time.Duration
}

func (o Options) timeout() time.Duration {
	if o.Timeout <= 0 {
		return 30 * time.Second
	}
	return o.Timeout
}

func buildArgs(command, file string, opts Options, extra ...string) []string {
	args := []string{command, "--format", "json"}
	if opts.Ruleset != "" {
		args = append(args, "--ruleset", opts.Ruleset)
	}
	if opts.Schema != "" {
		args = append(args, "--schema", opts.Schema)
	}
	if opts.Config != "" {
		args = append(args, "--config", opts.Config)
	}
	args = append(args, extra...)
	args = append(args, file)
	return args
}

func parseLintOutput(output []byte) (LintResult, error) {
	if len(output) == 0 {
		return LintResult{}, nil
	}

	// Try array format first
	var diagnostics []Diagnostic
	if err := json.Unmarshal(output, &diagnostics); err == nil {
		return LintResult{Diagnostics: diagnostics}, nil
	}

	// Try agent format: { diagnostics: [...], tokens: N }
	var result LintResult
	if err := json.Unmarshal(output, &result); err != nil {
		return LintResult{}, fmt.Errorf("failed to parse polychro output: %w", err)
	}
	return result, nil
}

// Lint validates a file and returns structured diagnostics.
func Lint(file string, opts Options) (LintResult, error) {
	binary, err := ResolveBinary()
	if err != nil {
		return LintResult{}, err
	}

	ctx, cancel := context.WithTimeout(context.Background(), opts.timeout())
	defer cancel()

	args := buildArgs("lint", file, opts)
	cmd := exec.CommandContext(ctx, binary, args...)
	output, err := cmd.Output()

	if ctx.Err() == context.DeadlineExceeded {
		return LintResult{}, fmt.Errorf("polychro binary timed out after %v", opts.timeout())
	}

	// Exit code 1 = diagnostics found (not an error)
	if err != nil {
		if exitErr, ok := err.(*exec.ExitError); ok {
			if exitErr.ExitCode() == 1 {
				return parseLintOutput(output)
			}
			return LintResult{}, fmt.Errorf("polychro failed (exit %d): %s",
				exitErr.ExitCode(), string(exitErr.Stderr))
		}
		return LintResult{}, fmt.Errorf("failed to execute polychro: %w", err)
	}

	return parseLintOutput(output)
}

// ValidateSchema runs schema validation only on a file.
func ValidateSchema(file string, opts Options) (LintResult, error) {
	binary, err := ResolveBinary()
	if err != nil {
		return LintResult{}, err
	}

	ctx, cancel := context.WithTimeout(context.Background(), opts.timeout())
	defer cancel()

	args := buildArgs("lint", file, opts, "--validators", "json-schema")
	cmd := exec.CommandContext(ctx, binary, args...)
	output, err := cmd.Output()

	if ctx.Err() == context.DeadlineExceeded {
		return LintResult{}, fmt.Errorf("polychro binary timed out after %v", opts.timeout())
	}

	if err != nil {
		if exitErr, ok := err.(*exec.ExitError); ok {
			if exitErr.ExitCode() == 1 {
				return parseLintOutput(output)
			}
			return LintResult{}, fmt.Errorf("polychro failed (exit %d): %s",
				exitErr.ExitCode(), string(exitErr.Stderr))
		}
		return LintResult{}, fmt.Errorf("failed to execute polychro: %w", err)
	}

	return parseLintOutput(output)
}

// ListRules returns the active rules (placeholder — returns empty until CLI exposes list-rules).
func ListRules(_ Options) ([]map[string]string, error) {
	return nil, nil
}
