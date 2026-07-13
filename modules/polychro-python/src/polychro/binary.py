# Copyright 2026 Naftiko — Apache License 2.0

"""Binary resolution for the Polychro native executable."""

from __future__ import annotations

import os
import platform
import shutil
from pathlib import Path


class BinaryNotFoundError(Exception):
    """Raised when the Polychro native binary cannot be located."""


def _platform_binary_name() -> str:
    """Return the expected binary name for the current platform."""
    system = platform.system().lower()
    if system == "windows":
        return "polychro.exe"
    return "polychro"


def _search_paths() -> list[Path]:
    """Return candidate paths where the binary might be found."""
    candidates: list[Path] = []

    # 1. POLYCHRO_BIN environment variable (explicit override)
    env_bin = os.environ.get("POLYCHRO_BIN")
    if env_bin:
        candidates.append(Path(env_bin))

    # 2. Adjacent to this package (bundled distribution)
    pkg_dir = Path(__file__).parent
    candidates.append(pkg_dir / "bin" / _platform_binary_name())

    # 3. On PATH
    on_path = shutil.which("polychro")
    if on_path:
        candidates.append(Path(on_path))

    return candidates


def resolve_binary() -> Path:
    """Locate the Polychro native binary.

    Search order:
    1. POLYCHRO_BIN environment variable
    2. Bundled binary adjacent to the package
    3. Binary on PATH

    Returns:
        Path to the executable.

    Raises:
        BinaryNotFoundError: if no binary is found in any location.
    """
    for candidate in _search_paths():
        if candidate.is_file() and os.access(candidate, os.X_OK):
            return candidate

    raise BinaryNotFoundError(
        "Polychro binary not found. Install it or set POLYCHRO_BIN. "
        "Searched: " + ", ".join(str(p) for p in _search_paths())
    )
