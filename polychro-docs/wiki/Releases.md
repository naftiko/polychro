# Releases

## Versioning

Polychro follows [Semantic Versioning](https://semver.org/):

- **MAJOR** — breaking API changes
- **MINOR** — new features, backward-compatible
- **PATCH** — bug fixes, backward-compatible

## Release History

### 0.1.0 (Upcoming)

First public release.

**Core:**
- `polychro-api` — SPI contracts (`Validator`, `Diagnostic`, `Document`, `ValidatorFactory`)
- `polychro-core` — Pipeline orchestrator with `ServiceLoader` discovery
- `polychro-wellformedness` — Duplicate key detection, encoding validation, depth limits
- `polychro-json-schema` — JSON Schema Draft 2020-12 validation
- `polychro-json-structure` — JSON Structure validation
- `polychro-ruleset` — Spectral-format ruleset engine with built-in functions
- `polychro-ruleset-polyglot` — JavaScript, Python, Groovy custom functions via GraalVM
- `polychro-rulesets` — Built-in governance, AI safety, and security rulesets
- `polychro-markdown` — Heading hierarchy, link validation, relative file references

**Distribution:**
- `polychro-cli` — Command-line interface with `text`, `json`, `sarif`, and `agent` output formats
- `polychro-capability` — MCP server mode (`polychro serve`)
- `polychro-github-action` — GitHub Action for CI integration
- Native binary compilation via GraalVM

**Rulesets:**
- `polychro:governance` — metadata, consumer, operations, orchestration, expose, tagging categories
- `polychro:ai-safety` — runtime failure detection beyond schema
- `polychro:security` — production hardening rules
