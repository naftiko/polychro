<img src="https://naftiko.github.io/docs/images/logos/logo_polychro_horizontal.png" width="300">

Welcome to Polychro, a deterministic linting engine for spec-driven development. Polychro validates semi-structured specifications such as YAML, JSON, and Markdown through composable layers including well-formedness, schema-model, ruleset, and format-aware validation in a single, embeddable pipeline.

When AI agents generate specifications at scale, deterministic linting is the essential safety net: the only layer that guarantees structural correctness and domain invariants before a spec reaches production.

| Feature | Description |
|---|---|
| CLI | Single binary — lint any YAML/JSON/Markdown spec from the command line |
| MCP Server Mode | Expose linting as MCP tools for AI agent consumption |
| Native Executable | Standalone binaries for **Linux**, **macOS**, and **Windows** — no JVM required |
| GitHub Action | Lint specs in CI with structured SARIF output |
| Spectral-Format Rulesets | Execute governance rulesets with `given`/`then` semantics |
| Polyglot Custom Functions | JavaScript, Python, and Groovy custom functions via sandboxed GraalVM |
| Schema-Model Validation | Formal document models including JSON Schema Draft 2020-12 and [JSON Structure](https://json-structure.org/) |
| Well-Formedness Validation | Duplicate keys, encoding, depth limits, YAML-specific traps |
| Format-Aware Validation | Heading hierarchy, internal links, relative file references, and other document-specific checks |
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
