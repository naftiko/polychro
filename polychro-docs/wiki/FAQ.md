# FAQ

## General

### What is Polychro?

Polychro is a deterministic linting engine for semi-structured specifications such as YAML, JSON, and Markdown. It combines well-formedness, schema-model, ruleset, and format-aware validation in a single pipeline — available as a CLI, an MCP server, a GitHub Action, or an embeddable Java library.

### Why not just use JSON Schema validation?

Schema validation ensures a document has the correct *shape* — required fields present, types correct. But it cannot catch *semantic* issues: a trailing slash on a URI that causes runtime failures, two adapters binding the same port, a step referencing a non-existent operation. Polychro's ruleset layer catches what schema alone cannot.

### Is Polychro tied to Naftiko?

No. Polychro is a standalone open-source library sponsored by [Naftiko](https://github.com/naftiko). Its core linting engine has no dependency on [Ikanos](https://github.com/naftiko/ikanos), [Naftiko Fleet](https://github.com/naftiko/fleet), or any other Naftiko product, and can lint semi-structured specifications such as YAML, JSON, and Markdown on its own. It ships with built-in rulesets optimized for [Ikanos](https://github.com/naftiko/ikanos) capability files, but the engine is format-agnostic and extensible through its validator SPI. You can write custom rulesets for OpenAPI, AsyncAPI, CloudEvents, or any specification.

### What languages can I write custom functions in?

JavaScript, Python, and Groovy — all executed via GraalVM's Polyglot API in a sandboxed context. Add the `polychro-ruleset-polyglot` module to enable polyglot functions.

### Does Polychro require Node.js?

No. Polychro is a self-contained binary (CLI) or JVM library. There is no dependency on Node.js, npm, or any JavaScript runtime — except for `polychro-checkov`, which requires a local Checkov installation (Python).

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
