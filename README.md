[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Coverage](https://img.shields.io/endpoint?url=https://raw.githubusercontent.com/naftiko/polychro/feat/removing-gist-logic/.github/badges/polychro-coverage.json)](https://github.com/naftiko/polychro/actions/workflows/nightly-quality-gate.yml)
[![Quality Gate](https://img.shields.io/endpoint?url=https://raw.githubusercontent.com/naftiko/polychro/feat/removing-gist-logic/.github/badges/polychro-quality-gate.json)](https://github.com/naftiko/polychro/actions/workflows/nightly-quality-gate.yml)
[![Bugs](https://img.shields.io/endpoint?url=https://raw.githubusercontent.com/naftiko/polychro/feat/removing-gist-logic/.github/badges/polychro-bugs.json)](https://github.com/naftiko/polychro/actions/workflows/nightly-quality-gate.yml)
[![Trivy](https://img.shields.io/endpoint?url=https://raw.githubusercontent.com/naftiko/polychro/feat/removing-gist-logic/.github/badges/polychro-trivy.json)](https://github.com/naftiko/polychro/actions/workflows/nightly-quality-gate.yml)
[![Gitleaks](https://img.shields.io/endpoint?url=https://raw.githubusercontent.com/naftiko/polychro/feat/removing-gist-logic/.github/badges/polychro-gitleaks.json)](https://github.com/naftiko/polychro/actions/workflows/nightly-quality-gate.yml)

**Polychro** is the open source AI linter for spec-driven development. **It lets your coding agent lint as you build, not hand it off to the one gating your CI pipeline** — in-process, multi-format, and agent-native, rebuilt for the flood of AI-generated specs that legacy OpenAPI-in-CI linters were never built to see. It shifts linting all the way left — out of the CI gate and into the coding agent's inner loop, catching a bad spec at agent speed, before it ever reaches a pull request.

<img src="https://naftiko.github.io/docs/images/technology/architecture_linting.png" width="800">

Polychro reads a specification — YAML, JSON, XML, Markdown, or HTML — and tells you what's wrong with it in a single, fast pass: structural mistakes, schema violations, broken conventions, and security anti-patterns. One binary, one config, one diagnostic format — fast enough to sit inside an AI agent's generate-validate-retry loop.

---

## What that means for *you*

- :robot: **You're building AI agents or tools** — give the agent a deterministic guardrail that lints its own output in the same turn, sub-second and in-process, or over MCP. → [MCP Server guide](https://shipyard.naftiko.io/polychro/1.0.0-beta1/guide/mcp-server/)
- :shield: **You run CI or a platform** — gate any spec on every PR with structured SARIF, and collapse a pile of linters into one binary, one config. → [GitHub Action guide](https://shipyard.naftiko.io/polychro/1.0.0-beta1/guide/github-action/)
- :triangular_ruler: **You author specs or rules** — reuse your Spectral rules, lint more than OpenAPI, and write custom rule functions in JavaScript, Python, Groovy, or Java. → [Rulesets guide](https://shipyard.naftiko.io/polychro/1.0.0-beta1/guide/rulesets/)

See **[Getting Started](https://shipyard.naftiko.io/polychro/1.0.0-beta1/getting-started/)** for the fastest path to your first lint.

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

For the full feature list see [Features](https://shipyard.naftiko.io/polychro/1.0.0-beta1/features/).

---

## What it is not

- **Not a meta-linter.** Super-Linter and MegaLinter bundle dozens of third-party tools in a large Docker image. Polychro is a single engine that owns the spec-validation slice end to end — no Docker dependency, no per-tool config to stitch together.
- **Not OpenAPI-only.** Polychro speaks the Spectral ruleset format, but treats arbitrary YAML, JSON, XML, Markdown, and HTML as first-class specs — capabilities, MCP manifests, CRDs, instruction files, not just OpenAPI.
- **Not a Node.js tool.** Polychro runs as a GraalVM-native binary with sub-second cold start, or in-process inside any JVM — no Node.js runtime, no 2–5 s subprocess startup.

---

## Continue reading

- [Getting Started](https://shipyard.naftiko.io/polychro/1.0.0-beta1/getting-started/) — install Polychro and run your first lint
- [Features](https://shipyard.naftiko.io/polychro/1.0.0-beta1/features/) — the full capability list
- [Architecture](https://shipyard.naftiko.io/polychro/1.0.0-beta1/architecture/) — how the validators compose into one pipeline
- [Tutorial](https://shipyard.naftiko.io/polychro/1.0.0-beta1/tutorial/) — an end-to-end linting workflow
- [Comparison](https://shipyard.naftiko.io/polychro/1.0.0-beta1/comparison/) — how Polychro compares to meta-linters and Spectral
- [Roadmap](https://shipyard.naftiko.io/polychro/1.0.0-beta1/roadmap/) — what's coming in upcoming releases

<img src="https://naftiko.github.io/docs/images/navi/navi_hello.svg" width="50">
