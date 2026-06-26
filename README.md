<img src="https://naftiko.github.io/docs/images/logos/logo_polychro_horizontal.png" width="300">

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Coverage](https://img.shields.io/endpoint?url=https://gist.githubusercontent.com/farah-t-trigui/7c4f0187bd2f5df1122abe46daa78a97/raw/polychro-coverage.json)](https://github.com/naftiko/polychro/actions/workflows/nightly-quality-gate.yml)
[![Bugs](https://img.shields.io/endpoint?url=https://gist.githubusercontent.com/farah-t-trigui/7c4f0187bd2f5df1122abe46daa78a97/raw/polychro-bugs.json)](https://github.com/naftiko/polychro/actions/workflows/nightly-quality-gate.yml)
[![Trivy](https://img.shields.io/endpoint?url=https://gist.githubusercontent.com/farah-t-trigui/7c4f0187bd2f5df1122abe46daa78a97/raw/polychro-trivy.json)](https://github.com/naftiko/polychro/actions/workflows/nightly-quality-gate.yml)
[![Gitleaks](https://img.shields.io/endpoint?url=https://gist.githubusercontent.com/farah-t-trigui/7c4f0187bd2f5df1122abe46daa78a97/raw/polychro-gitleaks.json)](https://github.com/naftiko/polychro/actions/workflows/nightly-quality-gate.yml)

**Polychro** is the open source AI linter for spec-driven development.

<img src="https://naftiko.github.io/docs/images/technology/architecture_linting.png" width="700">

Polychro reads a specification — YAML, JSON, XML, Markdown, or HTML — and tells you what's wrong with it in a single, fast pass: structural mistakes, schema violations, broken conventions, and security anti-patterns. One binary, one config, one diagnostic format — fast enough to sit inside an AI agent's generate-validate-retry loop.

---

## What that means for *you*

- :robot: **You're building AI agents or tools** — give the agent a deterministic guardrail that lints its own output in the same turn, sub-second and in-process, or over MCP. → [MCP Server guide](guide/mcp-server.md)
- :shield: **You run CI or a platform** — gate any spec on every PR with structured SARIF, and collapse a pile of linters into one binary, one config. → [GitHub Action guide](guide/github-action.md)
- :triangular_ruler: **You author specs or rules** — reuse your Spectral rules, lint more than OpenAPI, and write custom rule functions in JavaScript, Python, Groovy, or Java. → [Rulesets guide](guide/rulesets.md)

See **[Getting Started](getting-started.md)** for the fastest path to your first lint.

---

## Key features at a glance

| Feature | Description |
|---|---|
| **Polyglot Validation** | Lint **YAML**, **JSON**, **XML**, **Markdown**, and **HTML** specs through one pipeline |
| **Composable Pipeline** | Layered **well-formedness → schema-model → ruleset → format-aware** validation |
| **Native Executable** | Standalone binaries for **Linux**, **macOS**, and **Windows** — no JVM required |
| **MCP Server Mode** | Expose linting as **MCP** tools for AI agent consumption out of the box |
| **GitHub Action** | Lint specs in CI with structured **SARIF** output |
| **Spectral-Format Rulesets** | Execute governance rulesets with `given`/`then` semantics |
| **Polyglot Custom Functions** | Sandboxed **JavaScript**, **Python**, or **Groovy** rule functions via GraalVM |
| **Java Custom Functions** | Native `RuleFunction` SPI — fastest path, no GraalVM required |
| **Schema-Model Validation** | Formal document models including **JSON Schema Draft 2020-12** and [JSON Structure](https://json-structure.org/) |
| **Unified Diagnostics** | All validators produce the same `Diagnostic` format — one pipeline, one output |
| **Embeddable Java API** | In-process linting for JVM applications — no subprocess, no Node.js |

For the full feature list see [Features](features.md).

---

## What it is not

- **Not a meta-linter.** Super-Linter and MegaLinter bundle dozens of third-party tools in a large Docker image. Polychro is a single engine that owns the spec-validation slice end to end — no Docker dependency, no per-tool config to stitch together.
- **Not OpenAPI-only.** Polychro speaks the Spectral ruleset format, but treats arbitrary YAML, JSON, XML, Markdown, and HTML as first-class specs — capabilities, MCP manifests, CRDs, instruction files, not just OpenAPI.
- **Not a Node.js tool.** Polychro runs as a GraalVM-native binary with sub-second cold start, or in-process inside any JVM — no Node.js runtime, no 2–5 s subprocess startup.

---

## Continue reading

- [Getting Started](getting-started.md) — install Polychro and run your first lint
- [Features](features.md) — the full capability list
- [Architecture](architecture.md) — how the validators compose into one pipeline
- [Tutorial](tutorial.md) — an end-to-end linting workflow
- [Comparison](comparison.md) — how Polychro compares to meta-linters and Spectral
- [Roadmap](roadmap.md) — what's coming in upcoming alphas

<img src="https://naftiko.github.io/docs/images/navi/navi_hello.svg" width="50">
