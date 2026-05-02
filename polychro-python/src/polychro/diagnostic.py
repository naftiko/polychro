# Copyright 2026 Naftiko — Apache License 2.0

"""Data classes for Polychro diagnostics."""

from __future__ import annotations

import json
from dataclasses import dataclass, field
from enum import Enum
from typing import Optional


class Severity(Enum):
    """Diagnostic severity levels."""

    ERROR = "error"
    WARN = "warn"
    INFO = "info"
    HINT = "hint"


@dataclass(frozen=True)
class SourceRange:
    """Source location range for a diagnostic."""

    start_line: int
    start_character: int
    end_line: int
    end_character: int


@dataclass(frozen=True)
class Diagnostic:
    """A single lint diagnostic produced by Polychro."""

    severity: Severity
    code: str
    message: str
    path: str
    range: Optional[SourceRange] = None
    suggestion: Optional[str] = None

    def to_agent_format(self) -> str:
        """Format this diagnostic for LLM consumption (agent feedback loop).

        Returns a concise, structured string that an AI agent can parse
        to understand what went wrong and how to fix it.
        """
        parts = [
            f"[{self.severity.value.upper()}] {self.code}",
            f"  Path: {self.path}",
            f"  Message: {self.message}",
        ]
        if self.range:
            parts.append(
                f"  Location: line {self.range.start_line}:{self.range.start_character}"
            )
        if self.suggestion:
            parts.append(f"  Fix: {self.suggestion}")
        return "\n".join(parts)

    @classmethod
    def from_json(cls, data: dict) -> Diagnostic:
        """Parse a diagnostic from the CLI JSON output."""
        range_data = data.get("range")
        source_range = None
        if range_data and range_data.get("start"):
            source_range = SourceRange(
                start_line=range_data["start"].get("line", 0),
                start_character=range_data["start"].get("character", 0),
                end_line=range_data["end"].get("line", 0),
                end_character=range_data["end"].get("character", 0),
            )

        return cls(
            severity=Severity(data.get("severity", "error")),
            code=data.get("code", "unknown"),
            message=data.get("message", ""),
            path=data.get("path", ""),
            range=source_range,
            suggestion=data.get("suggestion"),
        )
