# What is Polychro

## The Problem

In spec-driven development, the specification *is* the product — AI agents generate it, orchestrate around it, and ship it. When non-deterministic agents produce YAML at scale, they generate structurally valid content that parses fine and even passes schema checks — but drifts on conventions, violates cross-object consistency rules, introduces security anti-patterns, and breaks invariants that only a domain-aware linter can catch.

Schema validation alone is not enough. A document can be schema-valid yet broken at runtime:

- A `baseUri` with a trailing slash causes double-slash path resolution failures
- Two exposed adapters binding the same port conflict silently at startup
- An input parameter declared but never referenced in any step wastes context and confuses agents
- A step referencing a non-existent operation passes schema but crashes at execution time

## The Solution

Polychro fills this gap with **multi-layer validation in a single pipeline**:

1. **Well-formedness** — catches YAML/JSON structural issues (duplicate keys, encoding problems, depth limits) before any semantic analysis begins
2. **Schema validation** — JSON Schema Draft 2020-12 conformance ensures the document has the correct shape
3. **JSON Structure** — optional strict typing layer for projects adopting the [JSON Structure](https://json-structure.org/) standard
4. **Ruleset linting** — Spectral-format governance rules with `given`/`then` semantics check cross-object consistency, naming conventions, security invariants, and domain-specific constraints
5. **Markdown validation** — heading hierarchy, internal links, and relative file references for documentation-as-code

All layers produce the same `Diagnostic` format. One API call, one result type, sub-second latency.

## Design Principles

1. **Agent-embeddable by design.** The primary consumer is an AI agent calling `Linter.lint()` in a tight generate-validate-retry loop. Latency, programmatic API ergonomics, and structured diagnostics matter more than terminal output.
2. **Deterministic guardrails for non-deterministic systems.** AI agents produce creative output. Polychro provides rigid, rule-based constraints that guarantee domain invariants hold — regardless of which model generated the spec.
3. **Modular by default.** Each validator is an independent module. Projects pick only what they need.
4. **SPI over configuration.** Validators are discovered via `ServiceLoader`, not hardcoded. Drop a JAR on the classpath and it participates in linting.
5. **100% test coverage.** Every module ships with complete line and branch coverage.

## Who is Polychro For?

| Audience | Use Case |
|---|---|
| **AI agent frameworks** | In-process validation in generate-validate-retry loops |
| **Spec-driven platforms** | Governance enforcement for YAML/JSON specifications |
| **API teams** | Linting OpenAPI, AsyncAPI, or custom specs with Spectral-format rules |
| **DevOps/Platform engineers** | CI gates via GitHub Action or CLI |
| **Tool authors** | Pluggable SPI for custom validation logic |

## Relationship to Naftiko

Polychro is a standalone library — it has no dependency on the Naftiko Framework and can lint any YAML or JSON document. It ships with built-in rulesets optimized for Naftiko capability files, but the engine itself is format-agnostic.

When used with the Naftiko Framework, Polychro provides the validation layer for capability files — both at development time (CLI, IDE) and at runtime (MCP server mode for AI agents).
