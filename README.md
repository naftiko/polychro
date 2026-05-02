# Polychro

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

Polychro is a deterministic linting engine for spec-driven development. It validates YAML and JSON specification files — schema conformance, cross-object consistency rules, document well-formedness, and structural checks — in a single, embeddable pipeline with sub-second latency.

Designed for AI agent loops where non-deterministic generation needs deterministic guardrails.

| Feature | Description |
|---|---|
| CLI | Single binary — lint any YAML/JSON spec from the command line |
| MCP Server Mode | Expose linting as MCP tools for AI agent consumption |
| Native Executable | GraalVM native-image compilation — no JVM required at runtime |
| GitHub Action | Lint specs in CI with structured SARIF output |
| Spectral-Format Rulesets | Execute governance rulesets with `given`/`then` semantics |
| Polyglot Custom Functions | JavaScript, Python, and Groovy custom functions via sandboxed GraalVM |
| JSON Schema Validation | Draft 2020-12 schema validation with structured diagnostics |
| JSON Structure Validation | Strict typing via the [JSON Structure](https://json-structure.org/) standard |
| Well-Formedness Checks | Duplicate keys, encoding, depth limits, YAML-specific traps |
| Markdown Linting | Heading hierarchy, internal links, relative file references |
| Unified Diagnostics | All validators produce the same `Diagnostic` format — one pipeline, one output |
| Embeddable Java API | In-process linting for JVM applications — no subprocess, no Node.js |
| Pluggable SPI | Add custom validators via `ServiceLoader` — zero framework coupling |

***

Here are additional documents to learn more:

- :compass: [What is Polychro?](https://github.com/naftiko/polychro/wiki/What-is-Polychro)
- :rowboat: [Getting Started](https://github.com/naftiko/polychro/wiki/Getting-Started)
- :sailboat: [Tutorial](https://github.com/naftiko/polychro/wiki/Tutorial)
- :ship: [Guide - Rulesets](https://github.com/naftiko/polychro/wiki/Guide-%E2%80%90-Rulesets)
- :wrench: [Guide - Configuration](https://github.com/naftiko/polychro/wiki/Guide-%E2%80%90-Configuration)
- :electric_plug: [Guide - MCP Server](https://github.com/naftiko/polychro/wiki/Guide-%E2%80%90-MCP-Server)
- :building_construction: [Architecture](https://github.com/naftiko/polychro/wiki/Architecture)
- :jigsaw: [Writing a Validator Plugin](https://github.com/naftiko/polychro/wiki/Writing-a-Validator-Plugin)
- :ocean: [FAQ](https://github.com/naftiko/polychro/wiki/FAQ)
- :mega: [Releases](https://github.com/naftiko/polychro/wiki/Releases)
- :telescope: [Roadmap](https://github.com/naftiko/polychro/wiki/Roadmap)
- :nut_and_bolt: [Contribute](https://github.com/naftiko/polychro/blob/main/CONTRIBUTING.md)

***

## Quick Start

### CLI

```bash
polychro lint my-spec.yml
polychro lint --ruleset polychro:governance my-spec.yml
polychro lint --format agent my-spec.yml
```

### MCP Server

```bash
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

### Java API

```xml
<dependency>
    <groupId>io.polychro</groupId>
    <artifactId>polychro-core</artifactId>
    <version>0.1.0</version>
</dependency>
```

```java
Linter linter = Linter.builder()
    .config(getClass().getResourceAsStream("/.polychro.yml"))
    .build();

Document doc = Document.fromString(yamlContent, "yaml");
List<Diagnostic> issues = linter.lint(doc);
```

***

## Module Overview

| Module | Purpose |
|---|---|
| `polychro-cli` | Command-line interface with native compilation |
| `polychro-capability` | MCP server mode for AI agents |
| `polychro-github-action` | GitHub Action for CI integration |
| `polychro-core` | Pipeline orchestrator — discovers and runs validators |
| `polychro-rulesets` | Curated governance and safety rulesets |
| `polychro-ruleset` | Spectral-format ruleset engine (built-in functions) |
| `polychro-ruleset-polyglot` | Polyglot custom functions (JS, Python, Groovy) |
| `polychro-json-schema` | JSON Schema Draft 2020-12 validation |
| `polychro-json-structure` | JSON Structure validation |
| `polychro-wellformedness` | Duplicate keys, encoding, depth limits |
| `polychro-markdown` | Markdown structural validation |
| `polychro-api` | SPI contracts: `Validator`, `Diagnostic`, `Document` |

***

## Built-in Rulesets

| Ruleset | Purpose |
|---|---|
| `polychro:governance` | Completeness and discoverability — metadata, consumers, operations, orchestration, exposed adapters |
| `polychro:ai-safety` | Catches patterns that fool schema validation but break at runtime — port conflicts, dangling references, circular dependencies |
| `polychro:security` | Hardened security posture for production specs — authentication enforcement, secret exposure prevention |

***

## Requirements

- **CLI**: Download the native binary — no JVM needed
- **MCP Server**: Same native binary, `polychro serve`
- **Java library**: Java 21+, Maven 3.9+
- **Polyglot functions**: GraalVM 21+ (optional)

## License

[Apache License 2.0](LICENSE.md)
