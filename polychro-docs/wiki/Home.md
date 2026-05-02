# Home

Welcome to Polychro, a deterministic linting engine for spec-driven development. Polychro validates YAML and JSON specification files — combining schema conformance, cross-object consistency rules, document well-formedness, and structural checks — in a single, embeddable pipeline.

When AI agents generate specifications at scale, deterministic linting is the essential safety net: the only layer that guarantees structural correctness and domain invariants before a spec reaches production.

| Feature | Description |
|---|---|
| CLI | Single binary — lint any YAML/JSON spec from the command line |
| MCP Server Mode | Expose linting as MCP tools for AI agent consumption |
| Native Executable | Standalone binaries for **Linux**, **macOS**, and **Windows** — no JVM required |
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

## Documentation

- :compass: [[What is Polychro]]
- :rowboat: [[Getting Started]]
- :sailboat: [[Tutorial]]
- :ship: [[Guide ‐ Rulesets]]
- :wrench: [[Guide ‐ Configuration]]
- :electric_plug: [[Guide ‐ MCP Server]]
- :building_construction: [[Architecture]]
- :jigsaw: [[Writing a Validator Plugin]]
- :ocean: [[FAQ]]
- :mega: [[Releases]]
- :telescope: [[Roadmap]]
- :nut_and_bolt: [Contribute](https://github.com/naftiko/polychro/blob/main/CONTRIBUTING.md)

***

## License

[Apache License 2.0](https://github.com/naftiko/polychro/blob/main/LICENSE.md)
