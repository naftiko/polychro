# Guide ‐ MCP Server

Polychro can run as an MCP (Model Context Protocol) server, exposing linting capabilities as tools that AI agents can invoke directly.

## Starting the Server

```bash
polychro serve
polychro serve --ruleset polychro:ai-safety
polychro serve --config .polychro.yml
```

## MCP Configuration

Add Polychro to your MCP client configuration:

```json
{
  "mcpServers": {
    "polychro": {
      "command": "polychro",
      "args": ["serve", "--ruleset", "polychro:ai-safety"]
    }
  }
}
```

## Available Tools

### `lint`

Full validation pipeline: well-formedness + schema + rules. Accepts a raw YAML or JSON string and returns diagnostics in agent format.

**Input:**
```json
{
  "document": "naftiko: 1.0.0-alpha1\nname: my-capability\n..."
}
```

**Output:**
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

### `validate-schema`

Schema-only validation — fast path for structural checks without ruleset evaluation.

**Input:**
```json
{
  "document": "name: my-capability\nversion: 1.0.0\n..."
}
```

### `list-rules`

Returns the active ruleset with rule names, descriptions, and severity levels. Agents use this to understand what constraints apply before generating.

**Output:**
```json
{
  "rules": [
    {
      "name": "capability-description-present",
      "description": "Every capability must include a human-readable description",
      "severity": "warning",
      "tags": ["metadata"]
    }
  ]
}
```

### `explain-diagnostic`

Given a diagnostic code, returns a natural-language explanation of the rule, why it matters, and a fix suggestion. Purpose-built for LLM consumption.

**Input:**
```json
{
  "code": "no-trailing-slash"
}
```

**Output:**
```json
{
  "rule": "no-trailing-slash",
  "explanation": "A baseUri ending with '/' causes double-slash when the engine concatenates it with operation paths. For example, 'https://api.example.com/' + '/users' becomes 'https://api.example.com//users'.",
  "fix": "Remove the trailing slash from the baseUri field.",
  "severity": "warning",
  "category": "consumer"
}
```

## Agent Workflow

A typical AI agent workflow with Polychro MCP:

1. Agent calls `list-rules` to understand active constraints
2. Agent generates a spec based on user intent
3. Agent calls `lint` with the generated spec
4. If diagnostics are returned, agent calls `explain-diagnostic` for unclear rules
5. Agent self-corrects and re-submits to `lint`
6. When no errors remain, the spec is finalized

## Configuration Flags

| Flag | Description | Default |
|---|---|---|
| `--ruleset` | Active ruleset (path or built-in name) | `polychro:governance` |
| `--schema` | Schema file for validation | none |
| `--config` | Configuration file path | `.polychro.yml` |

## Transport

Polychro MCP server uses **stdio** transport — standard input/output communication. This is the default transport for local MCP servers and is compatible with all MCP clients (Claude Desktop, VS Code, Cursor, etc.).
