# Roadmap

## Current Focus

### Core Library (v0.1.0)

- [x] SPI contracts and pipeline orchestrator
- [x] Well-formedness validation
- [x] JSON Schema validation (Draft 2020-12)
- [x] JSON Structure validation
- [x] Spectral-format ruleset engine
- [x] Polyglot custom functions (JS, Python, Groovy)
- [x] Java custom functions via `FunctionProvider` / `RuleFunction` SPI
- [x] Markdown validation
- [x] HTML validation (document / fragment / email / embedded-ui profiles)
- [x] Shared format utilities (anchors, link resolution, broken-link rules)
- [x] XML well-formedness (XXE / billion-laughs hardening) and ruleset validation via JSONPath
- [x] Built-in rulesets (governance, AI safety, security)
- [x] CLI with multiple output formats
- [x] MCP server mode
- [x] GitHub Action (composite, with glob expansion, `fail-on` threshold, SARIF upload to Code Scanning, PR summary)
- [x] Checkov external-process bridge (Terraform, Kubernetes, CloudFormation, Dockerfile, YAML)
- [x] Native binary compilation
- [x] SDK clients: Go, Node.js / TypeScript, Python

## Planned

### IDE Integration

- VS Code extension with real-time diagnostics
- IntelliJ plugin
- Language Server Protocol (LSP) support

### Ecosystem Expansion

- Additional built-in rulesets for OpenAPI and AsyncAPI
- Community ruleset registry
- Ruleset sharing and composition
- Additional HTML profiles (AMP, accessibility-strict)
- Optional external link reachability checks (off by default)
- Dedicated XML format-aware module (`polychro-xml`) — XSD validation, namespace consistency, DTD restrictions
- Additional external-process bridges (e.g. OPA / Conftest, kube-linter, tfsec)
- Native (non-composite) GitHub Action runtime for faster cold starts

### Performance

- Incremental validation (re-lint only changed sections)
- Parallel validator execution for large documents
- Caching layer for repeated validations

### Distribution

- Homebrew formula
- Docker image
- npm wrapper package (shipped — `polychro-node`)
- pip wrapper package (shipped — `polychro-python`)
- Go module (shipped — `polychro-go`)

## Contributing

See [CONTRIBUTING.md](https://github.com/naftiko/polychro/blob/main/CONTRIBUTING.md) for how to get involved. Feature requests and ruleset contributions are welcome via GitHub Issues.
