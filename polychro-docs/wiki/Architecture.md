# Architecture

## Execution Pipeline

Polychro runs validators in an ordered, composable sequence. Some layers are universal, while others apply only when a schema, stricter structure model, or format-specific validator is available for the document:

```
┌─────────────────────────────────────────────────────────────┐
│                     Document Input                          │
│         (YAML, JSON, Markdown, or other spec formats)       │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│              1. Well-Formedness Validation                  │
│     Duplicate keys, encoding, depth limits, format traps    │
│                                                             │
│     ⚡ Fail-fast: if errors here, skip remaining layers     │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│              2. Schema-Model Validation                     │
│     Formal document models, such as JSON Schema             │
│     or JSON Structure                                       │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│              3. Ruleset Validation                          │
│     Spectral-format rules with JSONPath + functions         │
│     Built-in functions + polyglot custom functions          │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│              4. Format-Aware Validation (if applicable)     │
│     Markdown heading hierarchy, link validation, etc.       │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│              Merged Diagnostics                             │
│     Deduplicated, sorted by severity, line-referenced       │
└─────────────────────────────────────────────────────────────┘
```

## Module Dependency Graph

```
polychro-api (SPI contracts)
    │
    ├── polychro-wellformedness
    ├── polychro-json-schema
    ├── polychro-json-structure
    ├── polychro-ruleset
    │       └── polychro-ruleset-polyglot (optional)
    ├── polychro-rulesets (resource-only)
    └── polychro-markdown
    
polychro-core (orchestrator)
    ├── depends on polychro-api
    └── discovers validators via ServiceLoader

polychro-cli
    └── depends on polychro-core

polychro-capability (MCP server)
    └── depends on polychro-core + Naftiko Framework

polychro-github-action
    └── depends on polychro-core
```

## Key Design Decisions

### ServiceLoader Discovery

Validators are discovered at runtime via `java.util.ServiceLoader`. Each validator module registers a `ValidatorFactory` in `META-INF/services/io.polychro.api.ValidatorFactory`.

This means:
- **No hardcoded validator list** — drop a JAR on the classpath and it participates
- **No annotation scanning** — pure SPI, no reflection at startup
- **Explicit ordering** — validators declare a `priority()` that controls execution order

### Fail-Fast Behavior

By default, well-formedness errors halt the pipeline. If a document has duplicate keys, schema validation results would be unreliable — so Polychro skips them.

Configure `failFast: false` to collect diagnostics from all layers regardless.

### Diagnostic Deduplication

When multiple validators report the same issue (e.g., a missing field caught by both schema and ruleset), Polychro deduplicates by `(code, range)` — keeping the most specific diagnostic (highest severity, most descriptive message).

### Document Abstraction

The `Document` interface abstracts over input sources. CLI and MCP server users never interact with it directly — the tool resolves documents from file paths or inline strings automatically. Plugin authors and Java API users work with `Document` when building custom validators:

```java
// From string
Document doc = Document.fromString(yaml, "yaml");

// From file
Document doc = Document.fromPath(Path.of("my-spec.yml"));

// From InputStream
Document doc = Document.fromStream(inputStream, "json");
```

All validators receive the same `Document` instance — parsed once, validated many times.

## SPI Contracts

> **Note:** This section is for plugin authors extending Polychro with custom validators. CLI, MCP server, and GitHub Action users do not need to interact with these interfaces.

### `Validator`

```java
public interface Validator {
    List<Diagnostic> validate(Document document, ValidatorConfig config);
    int priority();  // execution order (lower = earlier)
    String name();   // identifier for configuration
}
```

### `ValidatorFactory`

```java
public interface ValidatorFactory {
    Validator create(ValidatorConfig config);
    String name();
}
```

### `Diagnostic`

```java
public record Diagnostic(
    String code,
    Severity severity,
    String message,
    Range range,
    String source,    // which validator produced this
    String suggestion // optional fix hint
) {}
```

## Performance

Polychro is designed for sub-second latency — whether invoked from the CLI, an MCP tool call, or the Java API:

- **No cold start** — the native binary starts instantly; `Linter` instances are reusable and thread-safe in server and library modes
- **Lazy parsing** — documents are parsed on first access, cached thereafter
- **In-process by default** — core validators run entirely in the JVM, but the SPI allows external-process bridges (e.g., Checkov spawns a subprocess and parses its output back into diagnostics)
- **Polyglot sandboxing** — GraalVM contexts are pooled, not created per invocation
