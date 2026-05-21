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
- `polychro-ruleset` — Spectral-format ruleset engine with built-in functions, plus `FunctionProvider` SPI for native Java custom functions. Targets YAML, JSON, and XML structured trees.
- `polychro-ruleset-polyglot` — JavaScript, Python, Groovy custom functions via GraalVM
- `polychro-rulesets` — Built-in governance, AI safety, and security rulesets
- `polychro-markdown` — Heading hierarchy, link validation, relative file references
- `polychro-html` — HTML structure, accessibility, security, and asset rules across `document`, `fragment`, `email`, and `embedded-ui` profiles
- `polychro-format-common` — Shared cross-format utilities: anchor collection, link resolution, broken-local-reference and duplicate-anchor checks, opt-in external link probing
- `polychro-checkov` — Optional external-process bridge to [Checkov](https://www.checkov.io/) for security and compliance scanning. Framework auto-detection (Terraform, Kubernetes, CloudFormation, Dockerfile, YAML), configurable skip lists, custom-check directories, and graceful degradation when the `checkov` binary is absent.

**Distribution:**
- `polychro-cli` — Command-line interface with `text`, `json`, `sarif`, and `agent` output formats
- `polychro-capability` — MCP server mode (`polychro serve`)
- `polychro-github-action` — Composite GitHub Action with glob expansion, configurable `fail-on` severity threshold, SARIF output, automatic upload to GitHub Code Scanning, and PR-comment-ready Markdown summaries
- Native binary compilation via GraalVM

**SDK clients:**
- `polychro-go` — Go module (`go get github.com/naftiko/polychro/polychro-go`)
- `polychro-node` — npm package (`polychro`), Node.js 18+ / TypeScript
- `polychro-python` — PyPI package (`polychro`), Python 3.10+

**Format support:**
- YAML, JSON — well-formedness, schema-model (JSON Schema Draft 2020-12, JSON Structure), and ruleset
- XML — XXE / billion-laughs hardened well-formedness and ruleset (no schema-model or format-aware module yet)
- Markdown, HTML — well-formedness, projection-backed ruleset, and dedicated format-aware modules
