# Guide ‐ Configuration

## Configuration File

Polychro reads configuration from `.polychro.yml` at the project root. The file controls which validators run, their settings, and the overall pipeline behavior.

## Full Reference

```yaml
# .polychro.yml — full configuration reference

# Fail on the first error (skip remaining validators)
failFast: true

# Output format: text (default), json, sarif, agent
format: text

# Validators in execution order
validators:
  wellformedness:
    # Maximum nesting depth allowed
    maxDepth: 50
    # Maximum key length in characters
    maxKeyLength: 256
    # Reject documents with BOM markers
    rejectBom: true

  schema:
    # Path to JSON Schema file (Draft 2020-12)
    path: schemas/my-schema.json
    # Collect all errors vs stop at first (default: true)
    collectAllErrors: true

  jsonStructure:
    # Path to JSON Structure definition
    path: schemas/my-structure.json

  ruleset:
    # Path to ruleset file (Spectral format)
    path: rules/my-rules.yml
    # Directory containing custom function implementations
    functionsDir: rules/functions/
    # Only run rules with these tags
    includeTags:
      - security
      - metadata
    # Skip rules with these tags
    excludeTags:
      - experimental

  markdown:
    # Check relative file references exist
    checkRelativeLinks: true
    # Check external URLs are reachable
    checkExternalLinks: false
    # Timeout for external link checks (ms)
    externalLinkTimeout: 5000
    # Rate limit for external checks (requests/second)
    externalLinkRateLimit: 10
    # Base directory for resolving relative paths
    basePath: .
```

## CLI Flags

CLI flags override configuration file values:

| Flag | Description | Example |
|---|---|---|
| `--config` | Path to configuration file | `--config .polychro.yml` |
| `--schema` | Schema file path | `--schema my-schema.json` |
| `--ruleset` | Ruleset (path or built-in name) | `--ruleset polychro:ai-safety` |
| `--format` | Output format | `--format agent` |
| `--fail-fast` | Stop on first error | `--fail-fast` |
| `--tags` | Only run rules with these tags | `--tags security,metadata` |
| `--exclude-tags` | Skip rules with these tags | `--exclude-tags experimental` |
| `--severity` | Minimum severity to report | `--severity warn` |
| `--functions-dir` | Custom functions directory | `--functions-dir rules/functions/` |

## Programmatic API

```java
Linter linter = Linter.builder()
    // Load from config file
    .config(Path.of(".polychro.yml"))
    // Or configure programmatically
    .schema(Path.of("schemas/my-schema.json"))
    .ruleset("polychro:governance")
    .functionsDir(Path.of("rules/functions/"))
    .failFast(true)
    .build();
```

### Selective Validators

Include only the validators you need:

```java
// Schema-only validation (fast path)
Linter linter = Linter.builder()
    .schema(Path.of("my-schema.json"))
    .disableWellformedness(false)
    .disableRuleset(true)
    .build();
```

## Output Formats

### `text` (default)

Human-readable terminal output with colors and file/line references:

```
my-spec.yml
  2:1  warning  capability-description-present  Capability must include a description  
  5:3  error    consumer-base-uri-https         Consumer baseUri must use HTTPS         

✖ 2 problems (1 error, 1 warning)
```

### `json`

Machine-readable JSON array of diagnostics:

```json
[
  {
    "code": "capability-description-present",
    "severity": "warning",
    "message": "Capability must include a description",
    "path": "my-spec.yml",
    "range": { "start": { "line": 2, "col": 1 }, "end": { "line": 2, "col": 20 } }
  }
]
```

### `sarif`

SARIF 2.1.0 format for GitHub Code Scanning and IDE integration.

### `agent`

Token-efficient format optimized for AI agent consumption:

```json
{
  "diagnostics": [
    {
      "code": "capability-description-present",
      "severity": "warning",
      "message": "Capability must include a description",
      "range": { "start": { "line": 2, "col": 1 }, "end": { "line": 2, "col": 20 } },
      "suggestion": "Add a 'description' field with a meaningful summary"
    }
  ],
  "tokens": 47
}
```

## Environment Variables

| Variable | Description | Default |
|---|---|---|
| `POLYCHRO_CONFIG` | Path to configuration file | `.polychro.yml` |
| `POLYCHRO_FAIL_FAST` | Enable fail-fast mode | `false` |
| `POLYCHRO_FORMAT` | Output format | `text` |
