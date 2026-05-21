# FAQ

## General

### What is Polychro?

Polychro is a deterministic linting engine for semi-structured specifications such as YAML, JSON, XML, Markdown, and HTML. It combines well-formedness, schema-model, ruleset, and format-aware validation in a single pipeline — available as a CLI, an MCP server, a GitHub Action, an embeddable Java library, or as idiomatic SDKs for Go, Node.js / TypeScript, and Python.

### Why not just use JSON Schema validation?

Schema validation ensures a document has the correct *shape* — required fields present, types correct. But it cannot catch *semantic* issues: a trailing slash on a URI that causes runtime failures, two adapters binding the same port, a step referencing a non-existent operation. Polychro's ruleset layer catches what schema alone cannot.

### Is Polychro tied to Naftiko?

No — the core engine is independent. Polychro is a standalone open-source library sponsored by [Naftiko](https://github.com/naftiko). Its core linting engine (`polychro-core` and every validator module) has no dependency on [Ikanos](https://github.com/naftiko/ikanos), [Naftiko Fleet](https://github.com/naftiko/fleet), or any other Naftiko product, and can lint semi-structured specifications such as YAML, JSON, XML, Markdown, and HTML on its own.

The one exception is the **optional `polychro-capability` module**, which depends on [Ikanos](https://github.com/naftiko/ikanos) to expose Polychro as an MCP server (and Skills adapters). This dependency is mostly transparent: end users invoke `polychro serve` and the native binary or JAR transitively brings Ikanos in. If you only use the CLI for linting, the GitHub Action, or the embeddable Java API, no Ikanos dependency is pulled in.

Polychro ships with built-in rulesets optimized for [Ikanos](https://github.com/naftiko/ikanos) capability files, but the engine is format-agnostic and extensible through its validator SPI. You can write custom rulesets for OpenAPI, AsyncAPI, CloudEvents, or any specification.

### What languages can I write custom functions in?

**Java** is the recommended option — implement `RuleFunction`, expose it through a `FunctionProvider` registered via `META-INF/services`, and drop the JAR on the classpath. No GraalVM dependency, no sandbox overhead, and native compilation works out of the box.

For non-JVM authors, **JavaScript, Python, and Groovy** are supported via the optional `polychro-ruleset-polyglot` module, executed in a sandboxed GraalVM context.

### Does Polychro require Node.js?

No. Polychro is a self-contained binary (CLI) or JVM library. There is no dependency on Node.js, npm, or any JavaScript runtime — except for `polychro-checkov`, which requires a local Checkov installation (Python).

The `polychro-node` SDK is an *optional* npm package that wraps the native binary for Node.js / TypeScript callers; you only need Node.js if you choose to use that SDK.

### Can I use Polychro from Go, Node.js, or Python?

Yes — Polychro publishes three first-party SDKs that wrap the native binary:

| Language | Install | Import |
|---|---|---|
| Go (1.22+) | `go get github.com/naftiko/polychro/polychro-go` | `import "github.com/naftiko/polychro/polychro-go"` |
| Node.js (18+) / TypeScript | `npm install polychro` | `import { lint } from "polychro";` |
| Python (3.10+) | `pip install polychro` | `from polychro import Linter` |

All three SDKs expose the same surface — `lint`, `lintString` / `lint_string`, `validateSchema` / `validate_schema` — and return typed `LintResult` / `Diagnostic` objects. They locate the native binary via `POLYCHRO_BIN`, an adjacent `bin/` directory, or `PATH`.

### Does Polychro support XML?

Yes, partially. XML is a first-class structured format for **well-formedness** (the parser is XXE / billion-laughs hardened) and **ruleset validation** (JSONPath rules work against the Jackson tree produced by `XmlMapper`, with attributes addressed as `@name` fields). However, there is **no dedicated `polychro-xml` format-aware module** today — XSD / RelaxNG schema validation and XML-specific structural rules (namespace consistency, DTD checks, etc.) are not built in.

If your governance can be expressed as JSONPath rules, XML is fully supported via the ruleset engine — see [Guide ‐ Rulesets › Targeting XML Documents](Guide-‐-Rulesets#targeting-xml-documents). A dedicated XML format-aware module is tracked on the [Roadmap](Roadmap).

## Usage

### How do I integrate Polychro in my CI pipeline?

Use the GitHub Action:

```yaml
- uses: naftiko/polychro-action@v1
  with:
    files: "specs/**/*.yml"
    ruleset: polychro:ai-safety
    format: sarif
```

Or run the CLI directly:

```bash
polychro lint --format sarif specs/ > results.sarif
```

See [Guide ‐ GitHub Action](Guide-‐-GitHub-Action) for the complete inputs/outputs reference, glob and threshold semantics, and recipes.

### Does Polychro do security scanning?

Polychro's own validators focus on **structural linting and governance** (shape, naming, conventions). For **security and compliance** findings on infrastructure-as-code (Terraform, Kubernetes, CloudFormation, Dockerfile, YAML), the optional `polychro-checkov` module integrates [Checkov](https://www.checkov.io/) as an external-process validator:

```yaml
validators:
  checkov:
    enabled: true
```

Findings surface as standard Polychro `Diagnostic` objects — they appear in the same `text` / `json` / `sarif` / `agent` output and the same Code Scanning upload as your structural lint results. Requires `checkov` on PATH; missing-binary is tolerated with a single INFO diagnostic. See [Guide ‐ Checkov](Guide-‐-Checkov) for details.

### Can I use Polychro in an AI agent loop?

Yes — this is the primary design goal. Run `polychro serve` to start an MCP server, and invoke the `lint` tool from any MCP-compatible agent:

```bash
polychro serve --port 3001
```

For direct integration in JVM-based agents, use the Java API:

```java
Linter linter = Linter.builder().ruleset("polychro:ai-safety").build();
List<Diagnostic> issues = linter.lint(Document.fromString(yaml, "yaml"));
```

### How do I suppress a specific rule?

Exclude rules by tag or name via CLI:

```bash
polychro lint --exclude-tags experimental my-spec.yml
```

Or in a configuration file:

```yaml
validators:
  ruleset:
    excludeTags:
      - experimental
```

### What output formats are supported?

- `text` — human-readable terminal output (default)
- `json` — machine-readable JSON array
- `sarif` — SARIF 2.1.0 for GitHub Code Scanning
- `agent` — token-efficient format with suggestions and token count
- `html` — HTML format for web display

## Performance

### How fast is Polychro?

Full pipeline (well-formedness + schema + ruleset) typically completes in under 100ms for documents up to 1000 lines. The `Linter` instance is reusable and thread-safe — no cold start on subsequent calls.

### Can I run validators selectively?

Yes. Disable validators you don't need via CLI flags:

```bash
polychro lint --disable-ruleset --disable-markdown --schema my-schema.json my-spec.yml
```

Or via the Java API:

```java
Linter linter = Linter.builder()
    .schema(Path.of("my-schema.json"))
    .disableRuleset(true)
    .disableMarkdown(true)
    .build();
```

## Extending

### How do I add a custom validator?

Implement `ValidatorFactory` and `Validator`, register via `META-INF/services`, and add your JAR to the classpath. See [Writing a Validator Plugin](Writing-a-Validator-Plugin) for a complete walkthrough.

### Can I extend built-in rulesets?

Yes. Use `extends` in your ruleset file:

```yaml
extends:
  - polychro:governance

rules:
  my-custom-rule:
    given: $.metadata.team
    then:
      function: truthy
    message: Metadata must include a team field
    severity: warn
```
