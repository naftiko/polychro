# Architecture

## Execution Pipeline

Polychro runs validators in an ordered, composable sequence. Some layers are universal, while others apply only when a schema, stricter structure model, or format-specific validator is available for the document:

```
┌─────────────────────────────────────────────────────────────┐
│                     Document Input                          │
│      (YAML, JSON, XML, Markdown, HTML, or other formats)    │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│              1. Well-Formedness Validation                  │
│   Duplicate keys, encoding, depth limits, format traps,     │
│   XXE / billion-laughs hardening for XML                    │
│                                                             │
│     ⚡ Fail-fast: if errors here, skip remaining layers     │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│              2. Schema-Model Validation                     │
│     Formal document models, such as JSON Schema             │
│     or JSON Structure (YAML/JSON only)                      │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│              3. Ruleset Validation                          │
│     Spectral-format rules with JSONPath + functions         │
│     Targets YAML / JSON / XML structured trees              │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│              4. Format-Aware Validation (if applicable)     │
│     Markdown heading hierarchy, HTML accessibility, etc.    │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│              Merged Diagnostics                             │
│     Deduplicated, sorted by severity, line-referenced       │
└─────────────────────────────────────────────────────────────┘
```

### Format Coverage Matrix

Not every layer applies to every format. The following table shows which validators run for each input format:

| Format | Well-formedness | Schema-Model | Ruleset (JSONPath) | Format-aware |
|---|---|---|---|---|
| YAML | ✅ | ✅ JSON Schema, JSON Structure | ✅ | — |
| JSON | ✅ | ✅ JSON Schema, JSON Structure | ✅ | — |
| **XML** | ✅ (XXE-hardened) | ❌ (no XSD / RelaxNG today) | ✅ (attributes addressed as `@name`) | ❌ (no `polychro-xml` module today) |
| Markdown | ✅ | — | ✅ (via projection) | ✅ `polychro-markdown` |
| HTML | ✅ | — | ✅ (via projection) | ✅ `polychro-html` |

### Security Hardening for XML

`polychro-api` parses XML through a Jackson `XmlMapper` whose underlying `XMLInputFactory` is configured to disable external entity expansion (`XXE`), DTD loading, and unrestricted entity recursion (`billion laughs`). This means lint runs against untrusted XML — including agent-generated XML — cannot be used as a vector for entity-expansion DoS or out-of-band data exfiltration. The same hardening is applied to `Document.fromPath`, `Document.fromString`, and `Document.fromStream`.

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
    ├── polychro-format-common  (shared link / anchor utilities)
    │       │
    │       ├── polychro-markdown
    │       └── polychro-html
    └── (other validators…)
    
polychro-core (orchestrator)
    ├── depends on polychro-api
    └── discovers validators via ServiceLoader

polychro-checkov (external-process bridge)
    ├── depends on polychro-api
    └── shells out to a local `checkov` binary

polychro-cli
    └── depends on polychro-core

polychro-capability (MCP server)
    └── depends on polychro-core + Ikanos (for MCP / Skills adapters)

polychro-github-action
    └── depends on polychro-core
```

## SDK Clients

Beyond the JVM, Polychro publishes idiomatic SDKs that wrap the native binary so non-Java callers can consume the same diagnostics surface without a JVM dependency:

| SDK | Repository path | Distribution | Entry point |
|---|---|---|---|
| **Go** | `polychro-go/` | `go get github.com/naftiko/polychro/polychro-go` | `polychro.Lint(file, polychro.Options{...})` |
| **Node.js / TypeScript** | `polychro-node/` | `npm install polychro` | `await lint(file, { ... })` |
| **Python** | `polychro-python/` | `pip install polychro` | `Linter(...).lint(file)` |

All three SDKs share the same model:

1. Locate the native `polychro` binary in this order: `POLYCHRO_BIN` env var → adjacent `bin/` directory (bundled distribution) → `PATH`.
2. Shell out with `--format json` (or the agent format) and stream stdout back.
3. Parse the JSON / agent payload into typed `Diagnostic` / `LintResult` objects.
4. Map a CLI exit code of `1` (diagnostics found) to a successful return — non-zero codes greater than 1 surface as runtime errors with stderr captured.

Because the SDKs invoke the same binary the CLI ships, behaviour stays consistent across languages: rules, rulesets, profiles, schema validation, and output formats all match exactly.

## `polychro-format-common`

`polychro-format-common` is a **shared-rule library**, not a standalone validator. It has no `ValidatorFactory`, no SPI registration, no CLI surface, and no `validators.format-common` config key. It exists to give format validators (`polychro-markdown`, `polychro-html`, and any future format module) a single implementation of the cross-cutting checks that would otherwise drift apart.

### What lives there

| Class | Role |
|---|---|
| `AnchorCollector` | Walks a projected `Document` (via a per-format `DocumentReferenceAdapter`) and produces a normalised list of `Anchor` instances |
| `LinkResolver` | Resolves relative URLs against the document's `sourcePath` |
| `LinkReference` / `LinkKind` | Format-agnostic link descriptor (`INTERNAL_ANCHOR`, `RELATIVE_FILE`, `EXTERNAL`) |
| `MarkdownReferenceAdapter`, `HtmlReferenceAdapter` | Format-specific adapters consumed by `AnchorCollector` and `BrokenLocalReferenceRule` |
| `BrokenLocalReferenceRule` | Cross-format implementation behind `broken-relative-link` / `broken-relative-anchor` (Markdown) and `html-broken-fragment` / `html-missing-local-asset` (HTML) |
| `DuplicateAnchorRule` | Cross-format implementation behind `duplicate-anchor` (Markdown) and `html-duplicate-id` (HTML); supports companion-document lists for cross-file collision detection |
| `ExternalLinkProbe` / `ProbeResult` | Opt-in HTTP reachability cache; disabled by default to keep validation offline-deterministic |

### Design rationale

- **One bug-fix, many formats.** Anchor slug normalisation, link resolution rules, and external-link timeout behaviour are easy to get subtly inconsistent across format validators. Centralising them means a fix in `BrokenLocalReferenceRule` instantly applies to Markdown and HTML alike.
- **No format coupling.** `polychro-markdown` does not depend on `polychro-html` (and vice versa). They share code by both depending on the smaller `polychro-format-common` module — itself depending only on `polychro-api`.
- **Testable network behaviour.** `ExternalLinkProbe` injects its HTTP transport as a `Function`, so production code wires `HttpClient` while tests inject deterministic stubs.
- **Per-format error codes preserved.** The shared rules emit format-prefixed diagnostic codes through their callers; the SPI-visible code on each diagnostic stays format-specific (e.g. `html-duplicate-id` vs `duplicate-anchor`).

If you build a third format validator (e.g. RST, AsciiDoc), depend on `polychro-format-common` and supply a `DocumentReferenceAdapter` for your format — anchor and broken-link rules come for free.

## External-Process Validators

Not every useful linter is a Java library. To bring best-in-class tools written in other languages — security scanners, policy engines, syntax checkers — into the same diagnostic pipeline, Polychro supports an **external-process validator** pattern. The reference implementation lives in `polychro-checkov`.

### The pattern

An external-process validator implements the standard `io.polychro.api.Validator` SPI, but instead of doing work in-JVM it:

1. Builds a command line from the document path and validator config.
2. Spawns the external tool with `ProcessBuilder` and a configurable timeout.
3. Captures stdout (with `redirectErrorStream(true)`).
4. Parses the tool's structured output (JSON in Checkov's case) into a list of `Diagnostic` records via a dedicated `*OutputParser` + `DiagnosticMapper`.
5. **Degrades gracefully**: missing binary, missing file, and timeout each surface as a single `INFO` or `ERROR` diagnostic rather than throwing — the build never breaks just because an optional tool is absent.

### Why this is a separate module

`polychro-checkov` is **not** bundled into the core distribution by default. The pattern is deliberately opt-in because:

- **No transitive runtime dependency** on Python / a Checkov install — users who don't want it never see it.
- **Format detection lives next to the bridge**, not in `polychro-core`. `FrameworkDetector` knows that `.tf` → Terraform, `Dockerfile*` → Dockerfile, YAML containing `apiVersion:` → Kubernetes, YAML containing `AWSTemplateFormatVersion` → CloudFormation. This logic is meaningless without Checkov, so it stays in the Checkov module.
- **Severity mapping lives next to the bridge.** Checkov's `CRITICAL`/`HIGH`/`MEDIUM`/`LOW` vocabulary is mapped to Polychro's `Severity` enum inside `DiagnosticMapper` — different external tools will have different mappings.

The same pattern applies to any future external bridge: keep the executable path, timeout, mapping, and detection rules inside the bridge module; surface only `ValidatorFactory` + `META-INF/services` registration to the core.

### Trade-offs vs. in-process validators

| Concern | In-process (e.g. ruleset) | External-process (e.g. Checkov) |
|---|---|---|
| Startup | Instant | Process spawn per file (~50-200ms) |
| Dependency | JAR only | Requires external binary on PATH |
| Failure mode | Exception → ERROR diagnostic | Missing binary → INFO, timeout → ERROR |
| Update cadence | Tied to Polychro release | Independent (re-`pip install` the tool) |
| Parallelism | Threadsafe per-Validator | One subprocess per document (currently sequential) |

For latency-sensitive deployments (e.g. MCP tool calls), prefer in-process validators. For depth-first CI runs over IaC, the external bridge pattern is the right trade-off.

See [Guide ‐ Checkov](Guide-‐-Checkov.md) for end-user configuration.

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
