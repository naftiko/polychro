# Polychro Python SDK

Thin wrapper for the polychro CLI — deterministic linting for spec-driven development.

## Installation

```bash
pip install polychro
```

The SDK runs the `polychro` CLI, so install it first. It comes with its own Java runtime:

```bash
# macOS and Linux
curl -fsSL https://raw.githubusercontent.com/naftiko/polychro/main/scripts/install.sh | sh
```

```powershell
# Windows (PowerShell)
irm https://raw.githubusercontent.com/naftiko/polychro/main/scripts/install.ps1 | iex
```

Set `POLYCHRO_BIN` if the CLI is not on your `PATH`.

## Usage

```python
from polychro import Linter

linter = Linter(ruleset="naftiko-rules.yml")
result = linter.lint("capability.yml")

if result.has_errors:
    print(result.to_agent_format())
```
