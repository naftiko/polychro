# Tutorial

This tutorial walks through a complete linting workflow — from a simple CLI check to a multi-validator pipeline with custom rules and agent-optimized output.

## Step 1 — Lint a Spec

The simplest use case: lint a YAML file from the command line.

```bash
polychro lint my-spec.yml
```

Output:
```
my-spec.yml
  1:1  warning  capability-description-present  Capability must include a description

✖ 1 problem (0 errors, 1 warning)
```

Every issue produces a `Diagnostic` with:
- `severity` — ERROR, WARN, INFO, or HINT
- `message` — human-readable description
- `range` — line and column in the source document
- `code` — machine-readable diagnostic code

## Step 2 — Adding Ruleset Linting

Rulesets catch domain-specific issues that schema alone cannot detect:

```yaml
# .polychro.yml
validators:
  json-schema:
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
  json-schema:
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

The same rulesets are available through the MCP server (`polychro serve --ruleset polychro:ai-safety`) and the Java API (`Linter.builder().ruleset("polychro:ai-safety").build()`).

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

## Step 6 — MCP Server for AI Agents

Run Polychro as an MCP server so AI agents can lint specs directly:

```bash
polychro serve --ruleset polychro:ai-safety
```

Agents call the `lint` tool with a spec and get structured diagnostics back. They can also call `list-rules` to understand active constraints before generating, and `explain-diagnostic` to get fix suggestions for unclear rules.

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

## Step 8 — Java API Embedding

> For JVM applications that need in-process linting — such as agent frameworks or custom CI tools. Most users should use the CLI (Step 1) or MCP server (Step 6).

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

## Next Steps

- [Guide ‐ Rulesets](Guide-‐-Rulesets) — Deep dive into ruleset authoring
- [Guide ‐ Configuration](Guide-‐-Configuration) — Full configuration reference
- [Guide ‐ MCP Server](Guide-‐-MCP-Server) — Expose Polychro as MCP tools
