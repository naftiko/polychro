# Guide ‐ Rulesets

Polychro executes rulesets written in the Spectral format — `given`/`then` semantics with JSONPath targeting and built-in or custom functions.

## Ruleset Structure

```yaml
rules:
  rule-name:
    given: <JSONPath expression>
    then:
      field: <optional field to check>
      function: <function name>
      functionOptions: <optional parameters>
    message: <diagnostic message>
    severity: error | warn | info | hint
    description: <rule explanation>
    tags:
      - metadata
      - security
```

## Built-in Rulesets

### `polychro:governance`

The default governance ruleset. Structured into categories that mirror the specification's object model:

| Category | Rules | Governance Question |
|---|---|---|
| **Metadata** | `capability-name-present`, `capability-description-present`, `capability-version-format`, `capability-description-min-length` | Is the document identifiable and discoverable? |
| **Consumer** | `consumer-base-uri-no-trailing-slash`, `consumer-base-uri-not-example`, `consumer-base-uri-https`, `consumer-authentication-present`, `consumer-type-declared` | Are consumed APIs well-defined and production-ready? |
| **Operations** | `operation-description-present`, `operation-steps-non-empty`, `operation-output-parameters-defined`, `operation-input-used-in-steps` | Are operations complete and self-documenting? |
| **Orchestration** | `step-call-target-exists`, `step-output-mapping-valid`, `step-unreachable-detection`, `lookup-match-field-exists` | Is the step pipeline sound and wired correctly? |
| **Expose** | `expose-namespace-present`, `expose-port-unique`, `expose-description-present`, `expose-operations-non-empty` | Are exposed adapters properly configured? |
| **Tagging** | `capability-tags-present`, `operation-tags-present`, `tags-kebab-case`, `tags-unique` | Are documents discoverable via tagging? |

### `polychro:ai-safety`

Extends `polychro:governance` with rules that catch patterns which fool schema validation but break at runtime:

| Rule | Category | What it Catches |
|---|---|---|
| `no-trailing-slash` | Consumer | `baseUri` ends with `/` — double-slash in path resolution |
| `port-conflict` | Expose | Two adapters bind the same port |
| `unreachable-step` | Orchestration | A step exists but no execution path reaches it |
| `unused-parameter` | Operations | Input parameter declared but never referenced in steps |
| `dangling-reference` | Orchestration | A `call` references a non-existent operation |
| `empty-description` | Metadata | Description present but empty string |
| `duplicate-operation-name` | Operations | Ambiguous `call` targets at runtime |
| `circular-step-dependency` | Orchestration | Steps form a cycle — infinite loop at runtime |

### `polychro:security`

Production hardening rules:

| Rule | Severity | What it Catches |
|---|---|---|
| `auth-required` | ERROR | Consumed API has no authentication configured |
| `no-http-base-uri` | ERROR | `baseUri` uses `http://` instead of `https://` |
| `no-hardcoded-secrets` | ERROR | Credentials embedded directly in the spec |
| `expose-auth-present` | WARN | Exposed adapter has no server authentication |

## Built-in Functions

| Function | Purpose | Example |
|---|---|---|
| `truthy` | Field must be present and non-empty | Check description exists |
| `falsy` | Field must be absent or empty | Check deprecated fields removed |
| `pattern` | Field must match a regex | Version format validation |
| `enumeration` | Field must be one of allowed values | Type validation |
| `length` | String/array length constraints | Min/max description length |
| `schema` | Validate against an inline JSON Schema | Complex structure checks |
| `defined` | Field must be defined (even if null) | Required field presence |
| `undefined` | Field must not be defined | Banned field detection |
| `alphabetical` | Array/object keys must be sorted | Consistent ordering |

## Writing Custom Rules

### Simple Rule — Field Presence

```yaml
rules:
  my-description-required:
    given: $
    then:
      field: description
      function: truthy
    message: Document must include a description
    severity: warn
```

### Pattern Matching

```yaml
rules:
  my-version-format:
    given: $.version
    then:
      function: pattern
      functionOptions:
        match: "^\\d+\\.\\d+\\.\\d+$"
    message: Version must follow semver (e.g. 1.0.0)
    severity: error
```

### Multiple Assertions

```yaml
rules:
  my-consumer-https:
    given: $.consumes[*].baseUri
    then:
      function: pattern
      functionOptions:
        match: "^https://"
    message: Consumer baseUri must use HTTPS
    severity: error
```

## Custom Functions (Polyglot)

For rules that require complex logic beyond built-in functions, write custom functions in JavaScript, Python, or Groovy.

### JavaScript

```javascript
// functions/no-trailing-slash.js
export default function (input) {
  if (typeof input === 'string' && input.endsWith('/')) {
    return [{ message: `URI must not end with a trailing slash: ${input}` }];
  }
  return [];
}
```

### Python

```python
# functions/no_trailing_slash.py
def evaluate(input):
    if isinstance(input, str) and input.endswith('/'):
        return [{"message": f"URI must not end with a trailing slash: {input}"}]
    return []
```

### Groovy

```groovy
// functions/noTrailingSlash.groovy
def evaluate(input) {
    if (input instanceof String && input.endsWith('/')) {
        return [[message: "URI must not end with a trailing slash: ${input}"]]
    }
    return []
}
```

### Configuring Functions Directory

```yaml
validators:
  ruleset:
    path: my-rules.yml
    functionsDir: rules/functions/
```

Functions are resolved by name: a rule referencing `function: no-trailing-slash` loads `no-trailing-slash.js`, `no_trailing_slash.py`, or `noTrailingSlash.groovy` from the functions directory.

## Extending Built-in Rulesets

Reference a built-in ruleset and add custom rules:

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

## Tags and Filtering

Rules carry `tags` metadata for filtering:

```bash
# Lint only security-tagged rules
polychro lint --tags security my-spec.yml

# Exclude orchestration rules
polychro lint --exclude-tags orchestration my-spec.yml
```
