# Copyright 2026 Naftiko — Apache License 2.0

"""Core linting interface — subprocess wrapper around the Polychro native binary."""

from __future__ import annotations

import json
import subprocess
import tempfile
from dataclasses import dataclass
from pathlib import Path
from typing import Optional

from polychro.binary import resolve_binary
from polychro.diagnostic import Diagnostic


@dataclass(frozen=True)
class LintResult:
    """Result of a lint operation."""

    diagnostics: list[Diagnostic]
    tokens: int = 0

    @property
    def has_errors(self) -> bool:
        """True if any diagnostic has error severity."""
        from polychro.diagnostic import Severity

        return any(d.severity == Severity.ERROR for d in self.diagnostics)

    def to_agent_format(self) -> str:
        """Format all diagnostics for LLM consumption."""
        if not self.diagnostics:
            return "No issues found."
        parts = [d.to_agent_format() for d in self.diagnostics]
        parts.append(f"\nTotal: {len(self.diagnostics)} issue(s)")
        return "\n\n".join(parts)


class Linter:
    """Polychro linter — thin wrapper over the native binary.

    Usage:
        linter = Linter(ruleset="my-rules.yml")
        result = linter.lint("path/to/capability.yml")
        if result.has_errors:
            print(result.to_agent_format())
    """

    def __init__(
        self,
        *,
        ruleset: Optional[str] = None,
        schema: Optional[str] = None,
        config: Optional[str] = None,
        timeout: float = 30.0,
    ):
        self._binary = resolve_binary()
        self._ruleset = ruleset
        self._schema = schema
        self._config = config
        self._timeout = timeout

    def lint(self, file: str | Path) -> LintResult:
        """Lint a file and return structured diagnostics.

        Args:
            file: Path to the YAML/JSON file to lint.

        Returns:
            LintResult with parsed diagnostics.

        Raises:
            subprocess.TimeoutExpired: if the binary exceeds the timeout.
            RuntimeError: if the binary fails unexpectedly.
        """
        args = self._build_args("lint", str(file))
        output = self._run(args)
        return self._parse_lint_output(output)

    def lint_string(self, content: str, format: str = "yaml") -> LintResult:
        """Lint a string of YAML/JSON content.

        Writes content to a temporary file, lints it, and cleans up.

        Args:
            content: The YAML or JSON string to lint.
            format: File format hint ("yaml" or "json").

        Returns:
            LintResult with parsed diagnostics.
        """
        suffix = ".yml" if format == "yaml" else ".json"
        with tempfile.NamedTemporaryFile(
            mode="w", suffix=suffix, delete=False, encoding="utf-8"
        ) as f:
            f.write(content)
            tmp_path = Path(f.name)

        try:
            return self.lint(tmp_path)
        finally:
            tmp_path.unlink(missing_ok=True)

    def validate_schema(self, file: str | Path) -> LintResult:
        """Run schema validation only.

        Args:
            file: Path to the document to validate.

        Returns:
            LintResult with schema validation diagnostics only.
        """
        args = self._build_args("lint", str(file), extra=["--validators", "json-schema"])
        output = self._run(args)
        return self._parse_lint_output(output)

    def list_rules(self) -> list[dict]:
        """List all active rules in the configured ruleset.

        Returns:
            List of rule dictionaries with name, description, severity.
        """
        args = [str(self._binary), "lint", "--format", "json"]
        if self._ruleset:
            args.extend(["--ruleset", self._ruleset])
        # Use a dummy file to trigger rule listing — the CLI outputs rules
        # when format=json and no files are given (or with a special flag)
        # For now, we parse the ruleset directly
        # This is a placeholder until the CLI exposes a `list-rules` subcommand
        return []

    def _build_args(
        self, command: str, *positional: str, extra: Optional[list[str]] = None
    ) -> list[str]:
        """Build the CLI argument list."""
        args = [str(self._binary), command, "--format", "json"]
        if self._ruleset:
            args.extend(["--ruleset", self._ruleset])
        if self._schema:
            args.extend(["--schema", self._schema])
        if self._config:
            args.extend(["--config", self._config])
        if extra:
            args.extend(extra)
        args.extend(positional)
        return args

    def _run(self, args: list[str]) -> str:
        """Execute the binary and return stdout."""
        result = subprocess.run(
            args,
            capture_output=True,
            text=True,
            timeout=self._timeout,
        )
        # Exit code 0 = clean, 1 = diagnostics found, 2+ = error
        if result.returncode > 1:
            raise RuntimeError(
                f"Polychro binary failed (exit {result.returncode}): {result.stderr}"
            )
        return result.stdout

    def _parse_lint_output(self, output: str) -> LintResult:
        """Parse JSON output from the CLI into a LintResult."""
        if not output.strip():
            return LintResult(diagnostics=[])

        data = json.loads(output)

        if isinstance(data, list):
            diagnostics = [Diagnostic.from_json(d) for d in data]
            return LintResult(diagnostics=diagnostics)

        # Agent format has { diagnostics: [...], tokens: N }
        diagnostics_data = data.get("diagnostics", [])
        diagnostics = [Diagnostic.from_json(d) for d in diagnostics_data]
        tokens = data.get("tokens", 0)
        return LintResult(diagnostics=diagnostics, tokens=tokens)
