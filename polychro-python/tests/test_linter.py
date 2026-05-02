# Copyright 2026 Naftiko — Apache License 2.0

"""Tests for the Polychro Python SDK."""

import json
import subprocess
from pathlib import Path
from unittest.mock import MagicMock, patch

import pytest

from polychro.binary import BinaryNotFoundError, resolve_binary
from polychro.diagnostic import Diagnostic, Severity, SourceRange
from polychro.linter import Linter, LintResult


class TestDiagnostic:
    """Tests for the Diagnostic dataclass."""

    def test_from_json_minimal(self):
        data = {"severity": "error", "code": "test-rule", "message": "Broken", "path": "$.info"}
        d = Diagnostic.from_json(data)
        assert d.severity == Severity.ERROR
        assert d.code == "test-rule"
        assert d.message == "Broken"
        assert d.path == "$.info"
        assert d.range is None
        assert d.suggestion is None

    def test_from_json_with_range(self):
        data = {
            "severity": "warn",
            "code": "naming",
            "message": "Use kebab",
            "path": "$.info.name",
            "range": {
                "start": {"line": 5, "character": 2},
                "end": {"line": 5, "character": 10},
            },
        }
        d = Diagnostic.from_json(data)
        assert d.range is not None
        assert d.range.start_line == 5
        assert d.range.start_character == 2

    def test_from_json_with_suggestion(self):
        data = {
            "severity": "info",
            "code": "hint-rule",
            "message": "Consider adding tags",
            "path": "$.info",
            "suggestion": "Add a 'tags' array",
        }
        d = Diagnostic.from_json(data)
        assert d.suggestion == "Add a 'tags' array"

    def test_to_agent_format_basic(self):
        d = Diagnostic(
            severity=Severity.ERROR,
            code="no-trailing-slash",
            message="Remove trailing slash from baseUri",
            path="$.consumes[0].baseUri",
        )
        output = d.to_agent_format()
        assert "[ERROR] no-trailing-slash" in output
        assert "Path: $.consumes[0].baseUri" in output
        assert "Message: Remove trailing slash" in output

    def test_to_agent_format_with_range_and_suggestion(self):
        d = Diagnostic(
            severity=Severity.WARN,
            code="naming",
            message="Use kebab-case",
            path="$.info.name",
            range=SourceRange(start_line=3, start_character=4, end_line=3, end_character=12),
            suggestion="Rename to 'my-api'",
        )
        output = d.to_agent_format()
        assert "Location: line 3:4" in output
        assert "Fix: Rename to 'my-api'" in output


class TestLintResult:
    """Tests for LintResult."""

    def test_has_errors_true(self):
        result = LintResult(diagnostics=[
            Diagnostic(severity=Severity.ERROR, code="x", message="m", path="$"),
        ])
        assert result.has_errors is True

    def test_has_errors_false_when_only_warnings(self):
        result = LintResult(diagnostics=[
            Diagnostic(severity=Severity.WARN, code="x", message="m", path="$"),
        ])
        assert result.has_errors is False

    def test_has_errors_false_when_empty(self):
        result = LintResult(diagnostics=[])
        assert result.has_errors is False

    def test_to_agent_format_empty(self):
        result = LintResult(diagnostics=[])
        assert result.to_agent_format() == "No issues found."

    def test_to_agent_format_multiple(self):
        result = LintResult(diagnostics=[
            Diagnostic(severity=Severity.ERROR, code="a", message="first", path="$.x"),
            Diagnostic(severity=Severity.WARN, code="b", message="second", path="$.y"),
        ])
        output = result.to_agent_format()
        assert "Total: 2 issue(s)" in output
        assert "[ERROR] a" in output
        assert "[WARN] b" in output


class TestBinary:
    """Tests for binary resolution."""

    def test_binary_not_found_raises(self):
        with patch("polychro.binary.shutil.which", return_value=None):
            with patch.dict("os.environ", {}, clear=True):
                with pytest.raises(BinaryNotFoundError, match="Polychro binary not found"):
                    resolve_binary()

    def test_binary_found_via_env(self, tmp_path):
        binary = tmp_path / "polychro"
        binary.write_text("#!/bin/sh\n")
        binary.chmod(0o755)
        with patch.dict("os.environ", {"POLYCHRO_BIN": str(binary)}):
            result = resolve_binary()
            assert result == binary


class TestLinter:
    """Tests for the Linter subprocess wrapper."""

    def test_lint_valid_file(self, tmp_path):
        mock_output = json.dumps([])
        with patch("polychro.linter.resolve_binary", return_value=Path("/usr/bin/polychro")):
            with patch("subprocess.run") as mock_run:
                mock_run.return_value = MagicMock(
                    returncode=0, stdout=mock_output, stderr=""
                )
                linter = Linter()
                result = linter.lint(tmp_path / "test.yml")
                assert result.diagnostics == []
                assert result.has_errors is False

    def test_lint_invalid_file(self, tmp_path):
        mock_output = json.dumps([
            {"severity": "error", "code": "rule-1", "message": "Bad", "path": "$.info"},
        ])
        with patch("polychro.linter.resolve_binary", return_value=Path("/usr/bin/polychro")):
            with patch("subprocess.run") as mock_run:
                mock_run.return_value = MagicMock(
                    returncode=1, stdout=mock_output, stderr=""
                )
                linter = Linter()
                result = linter.lint(tmp_path / "test.yml")
                assert len(result.diagnostics) == 1
                assert result.has_errors is True

    def test_lint_timeout(self, tmp_path):
        with patch("polychro.linter.resolve_binary", return_value=Path("/usr/bin/polychro")):
            with patch("subprocess.run", side_effect=subprocess.TimeoutExpired("polychro", 30)):
                linter = Linter(timeout=30.0)
                with pytest.raises(subprocess.TimeoutExpired):
                    linter.lint(tmp_path / "test.yml")

    def test_lint_binary_error(self, tmp_path):
        with patch("polychro.linter.resolve_binary", return_value=Path("/usr/bin/polychro")):
            with patch("subprocess.run") as mock_run:
                mock_run.return_value = MagicMock(
                    returncode=2, stdout="", stderr="fatal error"
                )
                linter = Linter()
                with pytest.raises(RuntimeError, match="fatal error"):
                    linter.lint(tmp_path / "test.yml")

    def test_validate_schema(self, tmp_path):
        mock_output = json.dumps([])
        with patch("polychro.linter.resolve_binary", return_value=Path("/usr/bin/polychro")):
            with patch("subprocess.run") as mock_run:
                mock_run.return_value = MagicMock(
                    returncode=0, stdout=mock_output, stderr=""
                )
                linter = Linter(schema="naftiko-schema.json")
                result = linter.validate_schema(tmp_path / "test.yml")
                assert result.diagnostics == []
                # Verify --validators json-schema was passed
                call_args = mock_run.call_args[0][0]
                assert "--validators" in call_args
                assert "json-schema" in call_args

    def test_lint_with_agent_format_output(self, tmp_path):
        mock_output = json.dumps({
            "diagnostics": [
                {"severity": "warn", "code": "tags", "message": "Add tags", "path": "$.info"},
            ],
            "tokens": 12,
        })
        with patch("polychro.linter.resolve_binary", return_value=Path("/usr/bin/polychro")):
            with patch("subprocess.run") as mock_run:
                mock_run.return_value = MagicMock(
                    returncode=1, stdout=mock_output, stderr=""
                )
                linter = Linter()
                result = linter.lint(tmp_path / "test.yml")
                assert len(result.diagnostics) == 1
                assert result.tokens == 12

    def test_lint_string(self):
        mock_output = json.dumps([])
        with patch("polychro.linter.resolve_binary", return_value=Path("/usr/bin/polychro")):
            with patch("subprocess.run") as mock_run:
                mock_run.return_value = MagicMock(
                    returncode=0, stdout=mock_output, stderr=""
                )
                linter = Linter()
                result = linter.lint_string("naftiko: 1.0.0-alpha1\ninfo:\n  name: test\n")
                assert result.diagnostics == []
