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

## Custom Functions

For rules that require complex logic beyond built-in functions, write custom functions in **Java** (best performance, recommended for JVM authors) or in **JavaScript, Python, or Groovy** via the optional polyglot module.

| Language | Module | Discovery | Startup | Per-call cost |
|---|---|---|---|---|
| **Java** | `polychro-ruleset` (built-in) | `ServiceLoader` (`FunctionProvider`) | None | Native JVM call — fastest |
| JavaScript / Python / Groovy | `polychro-ruleset-polyglot` (optional) | `functionsDir` resolution | GraalVM context init | Sandboxed Polyglot call |

Prefer Java for production rulesets — there is no GraalVM dependency, no sandbox overhead, and native compilation works out of the box. Reach for polyglot when the rule author is not on the JVM, or when an existing script is being ported in.

### Java (Recommended)

Implement `RuleFunction` and expose it through a `FunctionProvider` registered via `META-INF/services`.

```java
// src/main/java/com/example/NoTrailingSlashFunction.java
package com.example;

import com.fasterxml.jackson.databind.JsonNode;
import io.polychro.ruleset.RuleFunction;

import java.util.List;
import java.util.Map;

public class NoTrailingSlashFunction implements RuleFunction {

    @Override
    public String name() {
        return "no-trailing-slash";
    }

    @Override
    public List<String> evaluate(JsonNode target, Map<String, Object> options) {
        if (target != null && target.isTextual() && target.asText().endsWith("/")) {
            return List.of("URI must not end with a trailing slash: " + target.asText());
        }
        return List.of();
    }
}
```

```java
// src/main/java/com/example/MyFunctionProvider.java
package com.example;

import io.polychro.ruleset.FunctionProvider;
import io.polychro.ruleset.RuleFunction;

import java.util.List;

public class MyFunctionProvider implements FunctionProvider {
    @Override
    public List<RuleFunction> functions() {
        return List.of(new NoTrailingSlashFunction());
    }
}
```

Register the provider:

```
# src/main/resources/META-INF/services/io.polychro.ruleset.FunctionProvider
com.example.MyFunctionProvider
```

Reference the function by name in any ruleset — no `functionsDir` configuration is required:

```yaml
rules:
  no-trailing-slash:
    given: $.consumes[*].baseUri
    then:
      function: no-trailing-slash
    severity: error
```

Drop the JAR on the classpath next to `polychro-core` and the function participates automatically.

### Polyglot (JavaScript, Python, Groovy)

Add the optional polyglot module to enable scripted functions:

```xml
<dependency>
    <groupId>io.polychro</groupId>
    <artifactId>polychro-ruleset-polyglot</artifactId>
    <version>0.1.0</version>
</dependency>
```

#### JavaScript

```javascript
// functions/no-trailing-slash.js
export default function (input) {
  if (typeof input === 'string' && input.endsWith('/')) {
    return [{ message: `URI must not end with a trailing slash: ${input}` }];
  }
  return [];
}
```

#### Python

```python
# functions/no_trailing_slash.py
def evaluate(input):
    if isinstance(input, str) and input.endswith('/'):
        return [{"message": f"URI must not end with a trailing slash: {input}"}]
    return []
```

#### Groovy

```groovy
// functions/noTrailingSlash.groovy
def evaluate(input) {
    if (input instanceof String && input.endsWith('/')) {
        return [[message: "URI must not end with a trailing slash: ${input}"]]
    }
    return []
}
```

#### Configuring the Functions Directory

```yaml
validators:
  ruleset:
    path: my-rules.yml
    functionsDir: rules/functions/
```

Functions are resolved by name: a rule referencing `function: no-trailing-slash` loads `no-trailing-slash.js`, `no_trailing_slash.py`, or `noTrailingSlash.groovy` from the functions directory. Polyglot functions require GraalVM 21+.

## Targeting XML Documents

XML is a first-class structured format for ruleset validation alongside YAML and JSON. `polychro-api` parses XML through a hardened Jackson `XmlMapper` (XXE / billion-laughs protections enabled) and projects it into the same Jackson tree the ruleset engine uses, so JSONPath `given` expressions work the same way they do for YAML or JSON.

A small XML configuration document:

```xml
<!-- service.xml -->
<service>
  <metadata>
    <name>orders</name>
    <description></description>
  </metadata>
  <consumes>
    <api baseUri="https://api.example.com/"/>
  </consumes>
</service>
```

A ruleset that catches the empty description and the trailing-slash `baseUri`:

```yaml
formats: [xml]

rules:
  service-description-present:
    given: $.metadata.description
    then:
      function: truthy
    message: Service must include a non-empty description
    severity: warn

  service-base-uri-no-trailing-slash:
    given: $.consumes.api['@baseUri']
    then:
      function: pattern
      functionOptions:
        notMatch: ".*/$"
    message: baseUri must not end with a trailing slash
    severity: error
```

Notes:

- Jackson's `XmlMapper` projects attributes as fields prefixed with `@` (e.g. `@baseUri`). Use the `['@name']` bracket syntax in JSONPath to address them.
- Repeated child elements collapse into arrays automatically — `$.consumes.api[*]` works for multiple `<api>` children.
- The `formats: [xml]` filter at the top of the ruleset scopes it so YAML/JSON inputs don't trigger XML-specific rules.

### What's Not Available for XML (Yet)

| Layer | Status |
|---|---|
| Well-formedness | ✅ Implicit — XXE-hardened parser fails fast on malformed XML |
| Schema-model | ❌ No XSD / RelaxNG validator module today |
| Ruleset | ✅ Full JSONPath ruleset support (this section) |
| Format-aware | ❌ No dedicated `polychro-xml` module (no namespace consistency, no DTD checks). Tracked on the [Roadmap](Roadmap) |

For now, model your XML governance as ruleset rules — the same SPI extension path that already covers YAML and JSON.

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
