# FAQ

## General

### What is Polychro?

Polychro is a deterministic linting engine for YAML and JSON specification files. It combines schema validation, ruleset linting, well-formedness checks, and structural validation in a single, embeddable pipeline.

### Why not just use JSON Schema validation?

Schema validation ensures a document has the correct *shape* — required fields present, types correct. But it cannot catch *semantic* issues: a trailing slash on a URI that causes runtime failures, two adapters binding the same port, a step referencing a non-existent operation. Polychro's ruleset layer catches what schema alone cannot.

### Is Polychro tied to Naftiko?

No. Polychro is a standalone library that can lint any YAML or JSON document. It ships with built-in rulesets optimized for Naftiko capability files, but the engine is format-agnostic. You can write custom rulesets for OpenAPI, AsyncAPI, CloudEvents, or any specification.

### What languages can I write custom functions in?

JavaScript, Python, and Groovy — all executed via GraalVM's Polyglot API in a sandboxed context. Add the `polychro-ruleset-polyglot` module to enable polyglot functions.

### Does Polychro require Node.js?

No. Polychro runs entirely on the JVM. There is no dependency on Node.js, npm, or any external process.

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

Yes — this is the primary design goal. The Java API returns structured diagnostics in sub-second latency:

```java
Linter linter = Linter.builder().ruleset("polychro:ai-safety").build();
List<Diagnostic> issues = linter.lint(Document.fromString(yaml, "yaml"));
```

For agents using MCP, run `polychro serve` and invoke the `lint` tool.

### How do I suppress a specific rule?

Exclude rules by tag or name in your configuration:

```yaml
validators:
  ruleset:
    excludeTags:
      - experimental
```

Or via CLI:

```bash
polychro lint --exclude-tags experimental my-spec.yml
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

Yes. Disable validators you don't need:

```java
Linter linter = Linter.builder()
    .schema(Path.of("my-schema.json"))
    .disableRuleset(true)
    .disableMarkdown(true)
    .build();
```

## Extending

### How do I add a custom validator?

Implement `ValidatorFactory` and `Validator`, register via `META-INF/services`, and add your JAR to the classpath. See [[Writing a Validator Plugin]] for a complete walkthrough.

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
