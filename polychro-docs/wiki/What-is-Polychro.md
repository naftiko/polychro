# What is Polychro

## The Problem

In spec-driven development, the specification *is* the product — AI agents generate it, orchestrate around it, and ship it. When non-deterministic agents produce YAML at scale, they generate structurally valid content that parses fine and even passes schema checks — but drifts on conventions, violates cross-object consistency rules, introduces security anti-patterns, and breaks invariants that only a domain-aware linter can catch.

Schema validation alone is not enough. A document can be schema-valid yet broken at runtime:

- A `baseUri` with a trailing slash causes double-slash path resolution failures
- Two exposed adapters binding the same port conflict silently at startup
- An input parameter declared but never referenced in any step wastes context and confuses agents
- A step referencing a non-existent operation passes schema but crashes at execution time

## The Solution

Polychro fills this gap with **composable validation layers applied consistently across semi-structured specifications** such as YAML, JSON, and Markdown:

1. **Well-formedness validation** — catches document-level structural issues such as duplicate keys, encoding problems, and depth limits before any semantic analysis begins
2. **Schema-model validation** — applies formal document models such as JSON Schema Draft 2020-12 or [JSON Structure](https://json-structure.org/), depending on what the specification adopts
3. **Ruleset linting** — evaluates cross-object consistency, naming conventions, security invariants, and domain-specific constraints through declarative rules
4. **Format-aware validation** — applies document-specific checks while preserving the same diagnostic model, such as heading hierarchy and hyperlink validation

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

Polychro is a standalone open-source library sponsored by [Naftiko](https://github.com/naftiko). Its core linting engine has no dependency on [Ikanos](https://github.com/naftiko/ikanos), [Naftiko Fleet](https://github.com/naftiko/fleet), or any other Naftiko product, and can lint semi-structured specifications such as YAML, JSON, and Markdown on its own.

It ships with built-in rulesets optimized for [Ikanos](https://github.com/naftiko/ikanos) capability files, and its optional capability module depends on [Ikanos](https://github.com/naftiko/ikanos) to expose MCP and Skills adapters, but the engine itself remains format-agnostic and extensible through its validator SPI.

[Ikanos](https://github.com/naftiko/ikanos) is Naftiko's sister open-source project for spec-driven integration, and Polychro can serve as its validation layer for capability files — both at development time (CLI, IDE) and at runtime (MCP server mode for AI agents).

[Naftiko](https://github.com/naftiko) is the vendor sponsoring both open-source projects and also offers [Naftiko Fleet](https://github.com/naftiko/fleet) as a product line for operating and governing capabilities at scale, with a free Community edition and premium editions. Polychro remains an independent open-source project whether or not you use [Ikanos](https://github.com/naftiko/ikanos) or [Naftiko Fleet](https://github.com/naftiko/fleet).
