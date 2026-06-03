<img src="https://naftiko.github.io/docs/images/logos/logo_polychro_horizontal.png" width="300">

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

[![Coverage](https://img.shields.io/endpoint?url=https://gist.githubusercontent.com/farah-t-trigui/7c4f0187bd2f5df1122abe46daa78a97/raw/polychro-coverage.json)](https://github.com/naftiko/polychro/actions/workflows/nightly-quality-gate.yml)
[![Bugs](https://img.shields.io/endpoint?url=https://gist.githubusercontent.com/farah-t-trigui/7c4f0187bd2f5df1122abe46daa78a97/raw/polychro-bugs.json)](https://github.com/naftiko/polychro/actions/workflows/nightly-quality-gate.yml)
[![Trivy](https://img.shields.io/endpoint?url=https://gist.githubusercontent.com/farah-t-trigui/7c4f0187bd2f5df1122abe46daa78a97/raw/polychro-trivy.json)](https://github.com/naftiko/polychro/actions/workflows/nightly-quality-gate.yml)
[![Gitleaks](https://img.shields.io/endpoint?url=https://gist.githubusercontent.com/farah-t-trigui/7c4f0187bd2f5df1122abe46daa78a97/raw/polychro-gitleaks.json)](https://github.com/naftiko/polychro/actions/workflows/nightly-quality-gate.yml)

Welcome to **Polychro**, the polyglot, deterministic linting engine for the AI era — validating any semi-structured specification, in-process, at the speed an agent can generate it.

> Polychro comes from the Greek *πολύχρωμος* — **many-colored**, for the many formats and many languages it lints.

<img src="https://naftiko.github.io/docs/images/technology/architecture_linting.png" width="700">

## What Polychro is

Polychro is the **spec-validation engine**. It reads a specification — **YAML**, **JSON**, **XML**, **Markdown**, or **HTML** — and runs it through a composable pipeline of validators (**well-formedness → schema-model → ruleset → format-aware**) with **sub-second latency and no subprocess required**. One config, one diagnostic format, one binary.

Each validator is a focused slice of the linting problem. The pipeline discovers them, runs them in order, and merges their findings into a single unified `Diagnostic` stream — so a YAML capability, a JSON Schema, a Markdown instruction file, and an HTML email are all linted the same way. The project ships everywhere it is needed: a **native binary**, a **JVM library**, an **MCP server**, a **GitHub Action**, and idiomatic **Go / Node.js / Python** SDKs.

Polychro is built for AI agent loops, where non-deterministic generation needs deterministic guardrails. An agent can generate a spec and have it validated in the same turn — fast enough to sit inside a tight tool-calling loop, and callable directly over MCP.

## What Polychro is *not*

- **Not a meta-linter.** Super-Linter and MegaLinter bundle dozens of third-party tools in a large Docker image and orchestrate them in CI. Polychro is a single engine, not an orchestrator: it owns the spec-validation slice end to end — well-formedness, schema, rulesets, and format-aware checks — in one binary, with no Docker dependency and no per-tool config to stitch together.
- **Not OpenAPI-only.** Spectral-family rule engines lint API contracts. Polychro speaks the same Spectral ruleset format, but treats arbitrary YAML, JSON, Markdown, and HTML as first-class specs — capabilities, MCP manifests, CRDs, instruction files, and documentation, not just OpenAPI.
- **Not a Node.js tool.** Polychro runs as a GraalVM-native binary with sub-second cold start, or in-process inside any JVM application — no Node.js runtime, no 2–5 s subprocess startup, usable inside an agent loop.

## Key features at a glance

| Feature | Description |
|---|---|
| Polyglot Validation | Lint **YAML**, **JSON**, **XML**, **Markdown**, and **HTML** specs through one pipeline |
| Composable Pipeline | Layered **well-formedness → schema-model → ruleset → format-aware** validation |
| Native Executable | Standalone binaries for **Linux**, **macOS**, and **Windows** — no JVM required |
| MCP Server Mode | Expose linting as **MCP** tools for AI agent consumption out of the box |
| GitHub Action | Lint specs in CI with structured **SARIF** output |
| Spectral-Format Rulesets | Execute governance rulesets with `given`/`then` semantics |
| Polyglot Custom Functions | Embed sandboxed **JavaScript**, **Python**, or **Groovy** rule functions (GraalVM) |
| Java Custom Functions | Native `RuleFunction` SPI — fastest path, no GraalVM required |
| Schema-Model Validation | Formal document models including **JSON Schema Draft 2020-12** and [JSON Structure](https://json-structure.org/) |
| Well-Formedness Validation | Duplicate keys, encoding, depth limits, and YAML-specific traps |
| Format-Aware Validation | Heading hierarchy, internal links, relative file references, HTML structure / accessibility / security |
| Unified Diagnostics | All validators produce the same `Diagnostic` format — one pipeline, one output |
| Embeddable Java API | In-process linting for JVM applications — no subprocess, no Node.js |
| SDK Clients | Idiomatic wrappers for **Go**, **Node.js / TypeScript**, and **Python** over the native binary |
| Pluggable SPI | Add custom validators via `ServiceLoader` — zero framework coupling |

## How Polychro compares

The "lint your specs" space mostly solves a 2019 problem: *how do we govern OpenAPI files in CI?* Polychro is built for the 2026 problem: *how do we validate any spec, in-process, at the speed an AI agent generates it?* The market splits into three layers, and Polychro deliberately occupies the bottom one:

- **Meta-linters / orchestrators** — *Super-Linter, MegaLinter.* Large Docker images that bundle dozens of third-party linters and run them in CI. They *delegate* spec linting to a rule engine.
- **Spectral-family rule engines** — *Spectral (Node), Vacuum (Go), Redocly Lint (TypeScript).* Single-purpose linters for OpenAPI / AsyncAPI rulesets.
- **Embeddable, polyglot, agent-era spec engines** — *Polychro.* A composable pipeline that runs as a native binary, a JVM library, an MCP server, or a GitHub Action.

Polychro replaces the *rule-engine layer* and absorbs the *spec-validation slice* of the meta-linters — collapsing what is today three or four tools, two or three config files, and a Docker dependency into one binary, one config, one diagnostic format, sub-second. Because it speaks the **Spectral ruleset format** natively, existing rule investment carries over unchanged.

> For a dimension-by-dimension comparison, and guidance on when another tool is a better fit, see the [Comparison guide](https://shipyard.naftiko.io/docs/1.0.0-alpha3/polychro/comparison/) on the Shipyard.

## Quick start

You can run Polychro with the **native CLI**, as an **MCP server**, or embed it as a **JVM library**. The CLI has the least friction, so start there.

### With the CLI

```bash
# Lint any spec — Polychro detects the format
polychro lint my-spec.yml

# Apply a curated governance ruleset
polychro lint --ruleset polychro:governance my-spec.yml

# Validate an HTML email with the email profile
polychro lint --validator html=email welcome-email.html

# Emit agent-optimized output for an AI loop
polychro lint --format agent my-spec.yml
```

### As an MCP server

```bash
# Serve linting as MCP tools (Ctrl-C to stop)
polychro serve --ruleset polychro:ai-safety
```

```json
{
  "mcpServers": {
    "polychro": {
      "command": "polychro",
      "args": ["serve", "--ruleset", "polychro:ai-safety"]
    }
  }
}
```

### As a Java library

```xml
<dependency>
    <groupId>io.polychro</groupId>
    <artifactId>polychro-core</artifactId>
    <version>1.0.0-alpha3</version>
</dependency>
```

```java
Linter linter = Linter.builder()
    .config(getClass().getResourceAsStream("/.polychro.yml"))
    .build();

Document doc = Document.fromString(yamlContent, "yaml");
List<Diagnostic> issues = linter.lint(doc);
```

### From Go, Node.js, or Python

```bash
go get github.com/naftiko/polychro/polychro-go   # Go
npm install polychro                              # Node.js / TypeScript
pip install polychro                              # Python
```

```python
# Python example — same surface in Go and Node.js
from polychro import Linter

linter = Linter(ruleset="polychro:ai-safety")
result = linter.lint("my-spec.yml")
if result.has_errors:
    print(result.to_agent_format())
```

> Full installation steps for the CLI, the SDKs, and every platform are in the [Installation guide](https://shipyard.naftiko.io/docs/1.0.0-alpha3/polychro/installation/) on the Shipyard.

***

## :anchor: Dive deeper on the Shipyard

The complete, always-up-to-date Polychro documentation lives on the **Naftiko Shipyard**. Start here and keep exploring:

- :compass: [Concepts — Spec-Driven Integration](https://shipyard.naftiko.io/docs/1.0.0-alpha3/concepts/spec-driven-integration/)
- :rowboat: [Installation](https://shipyard.naftiko.io/docs/1.0.0-alpha3/polychro/installation/)
- :speedboat: [Getting Started](https://shipyard.naftiko.io/docs/1.0.0-alpha3/polychro/getting-started/)
- :sailboat: [Tutorial](https://shipyard.naftiko.io/docs/1.0.0-alpha3/polychro/tutorial/)
- :star: [Features](https://shipyard.naftiko.io/docs/1.0.0-alpha3/polychro/features/)
- :building_construction: [Architecture](https://shipyard.naftiko.io/docs/1.0.0-alpha3/polychro/architecture/)
- :keyboard: [CLI Reference](https://shipyard.naftiko.io/docs/1.0.0-alpha3/polychro/cli/)
- :ship: [Guide — Rulesets](https://shipyard.naftiko.io/docs/1.0.0-alpha3/polychro/guide/rulesets/)
- :wrench: [Guide — Configuration](https://shipyard.naftiko.io/docs/1.0.0-alpha3/polychro/guide/configuration/)
- :electric_plug: [Guide — MCP Server](https://shipyard.naftiko.io/docs/1.0.0-alpha3/polychro/guide/mcp-server/)
- :jigsaw: [Guide — Validator Plugin](https://shipyard.naftiko.io/docs/1.0.0-alpha3/polychro/guide/validator-plugin/)
- :octocat: [Guide — GitHub Action](https://shipyard.naftiko.io/docs/1.0.0-alpha3/polychro/guide/github-action/)
- :ocean: [FAQ](https://shipyard.naftiko.io/docs/1.0.0-alpha3/polychro/faq/)
- :mega: [Releases](https://shipyard.naftiko.io/docs/1.0.0-alpha3/polychro/releases/)
- :telescope: [Roadmap](https://shipyard.naftiko.io/docs/1.0.0-alpha3/polychro/roadmap/)
- :nut_and_bolt: [Contribute](https://github.com/naftiko/polychro/blob/main/CONTRIBUTING.md)

## :video_game: Try it in the Playground

Want to see Polychro in action without installing anything? The upcoming **Shipyard Playground** lets you author a spec, lint it live with **Polychro**, and serve the result with **Ikanos** — all from your browser. Keep an eye on the [Shipyard](https://shipyard.naftiko.io/docs/1.0.0-alpha3/) for its release.

***

## Built-in rulesets

| Ruleset | Purpose |
|---|---|
| `polychro:governance` | Completeness and discoverability — metadata, consumers, operations, orchestration, exposed adapters |
| `polychro:ai-safety` | Catches patterns that fool schema validation but break at runtime — port conflicts, dangling references, circular dependencies |
| `polychro:security` | Hardened security posture for production specs — authentication enforcement, secret exposure prevention |

***

## Part of the Naftiko Fleet

Polychro is part of the [Naftiko Fleet (Community Edition)](https://shipyard.naftiko.io/docs/1.0.0-alpha3/fleet/), which adds free complementary tools:

| Tool | What it does |
|---|---|
| [Ikanos](https://shipyard.naftiko.io/docs/1.0.0-alpha3/ikanos/) | Capability engine for Spec-Driven Integration — Polychro lints the capabilities it serves. |
| [Crafter](https://shipyard.naftiko.io/docs/1.0.0-alpha3/fleet/crafter/) | Free Naftiko extension for Visual Studio Code to help with editing and linting specs. |
| [Warden](https://shipyard.naftiko.io/docs/1.0.0-alpha3/fleet/) | Naftiko custom templates for CNCF's Backstage to help with scaffolding and cataloguing capabilities. |
| [Skipper](https://shipyard.naftiko.io/docs/1.0.0-alpha3/fleet/skipper/) | Operator and Helm chart for CNCF's Kubernetes to help with the operations of Ikanos capabilities. |

Please join the community of users and contributors in [this GitHub Discussion forum!](https://github.com/orgs/naftiko/discussions)

<img src="https://naftiko.github.io/docs/images/navi/navi_hello.svg" width="50">
