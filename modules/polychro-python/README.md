# Polychro Python SDK

Thin wrapper for the Polychro native binary — deterministic linting for spec-driven development.

## Installation

```bash
pip install polychro
```

## Usage

```python
from polychro import Linter

linter = Linter(ruleset="naftiko-rules.yml")
result = linter.lint("capability.yml")

if result.has_errors:
    print(result.to_agent_format())
```
