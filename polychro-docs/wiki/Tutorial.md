# Tutorial

This tutorial walks through a complete linting workflow — from a simple schema check to a multi-validator pipeline with custom rules and agent-optimized output.

## Step 1 — Schema Validation

The simplest use case: validate a YAML document against a JSON Schema.

```java
Linter linter = Linter.builder()
    .config("""
        validators:
          schema:
            path: my-schema.json
        """)
    .build();

Document doc = Document.fromPath(Path.of("my-spec.yml"));
List<Diagnostic> issues = linter.lint(doc);
```

Every schema violation produces a `Diagnostic` with:
- `severity` — ERROR for schema violations
- `message` — human-readable description
- `range` — line and column in the source document
- `code` — machine-readable diagnostic code

## Step 2 — Adding Ruleset Linting

Rulesets catch domain-specific issues that schema alone cannot detect:

```yaml
# .polychro.yml
validators:
  schema:
    path: my-schema.json
  ruleset:
    path: my-rules.yml
```

```yaml
# my-rules.yml
rules:
  description-present:
    given: $
    then:
      field: description
      function: truthy
    message: Document must include a description
    severity: warn
```

## Step 3 — Well-Formedness as First Gate

Well-formedness checks run before schema and ruleset validation. If a document has duplicate keys or encoding issues, there's no point running semantic checks:

```yaml
validators:
  wellformedness:
    maxDepth: 50
  schema:
    path: my-schema.json
  ruleset:
    path: my-rules.yml
```

The pipeline short-circuits on well-formedness errors by default. Configure `failFast: false` to collect all diagnostics regardless.

## Step 4 — Using Built-in Rulesets

Polychro ships with curated rulesets ready to use:

```bash
# Governance ruleset — completeness and discoverability
polychro lint --ruleset polychro:governance my-spec.yml

# AI safety ruleset — catches runtime failures that fool schema
polychro lint --ruleset polychro:ai-safety my-spec.yml

# Security ruleset — production hardening
polychro lint --ruleset polychro:security my-spec.yml
```

Via the Java API:

```java
Linter linter = Linter.builder()
    .ruleset("polychro:ai-safety")
    .build();
```

## Step 5 — Polyglot Custom Functions

For complex cross-object rules, write custom functions in JavaScript, Python, or Groovy:

```javascript
// functions/check-port-unique.js
export default function (input) {
  const ports = input.map(adapter => adapter.port);
  const duplicates = ports.filter((p, i) => ports.indexOf(p) !== i);
  if (duplicates.length > 0) {
    return [{ message: `Duplicate port: ${duplicates[0]}` }];
  }
  return [];
}
```

Reference in your ruleset:

```yaml
rules:
  port-unique:
    given: $.exposes[*]
    then:
      function: check-port-unique
    severity: error
```

Configure the functions directory:

```yaml
validators:
  ruleset:
    path: my-rules.yml
    functionsDir: rules/functions/
```

## Step 6 — Agent Feedback Loop

The real power of Polychro: embedding it in an AI agent's generate-validate-retry loop.

```java
Linter linter = Linter.builder()
    .ruleset("polychro:ai-safety")
    .schema(schemaPath)
    .build();

// Agent generates a spec
String yaml = agent.generate(userIntent);
Document doc = Document.fromString(yaml, "yaml");

// Validate — all layers in one call, <100ms
List<Diagnostic> issues = linter.lint(doc);

if (issues.stream().anyMatch(d -> d.severity() == Severity.ERROR)) {
    // Feed diagnostics back to the agent for self-correction
    agent.retry(userIntent, issues);
} else {
    // Spec is valid — proceed
    deploy(doc);
}
```

## Step 7 — Agent-Optimized Output Format

For AI agents consuming diagnostics, the `agent` format produces token-efficient JSON:

```bash
polychro lint --format agent my-spec.yml
```

```json
{
  "diagnostics": [
    {
      "code": "capability-description-present",
      "severity": "warning",
      "message": "Capability must include a description",
      "range": { "start": { "line": 1, "col": 1 }, "end": { "line": 1, "col": 20 } },
      "suggestion": "Add a 'description' field with a meaningful summary"
    }
  ],
  "tokens": 47
}
```

The `tokens` field pre-computes the context window cost — agents can decide whether to include all diagnostics or summarize.

## Next Steps

- [[Guide ‐ Rulesets]] — Deep dive into ruleset authoring
- [[Guide ‐ Configuration]] — Full configuration reference
- [[Guide ‐ MCP Server]] — Expose Polychro as MCP tools
