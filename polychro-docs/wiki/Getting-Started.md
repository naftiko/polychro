# Getting Started

## Prerequisites

- **CLI / MCP Server**: Download the native binary — no JVM needed
- **Java library**: Java 21+, Maven 3.9+
- **Polyglot functions**: GraalVM 21+ (optional)
- **SDK clients**: Go 1.22+, Node.js 18+, or Python 3.10+ (the SDK shells out to the native binary, which must be installed separately)

## Installation

### CLI (Native Binary)

Download the native binary for your platform from the [Releases](https://github.com/naftiko/polychro/releases) page. No JVM required.

```bash
# Linux / macOS
chmod +x polychro
./polychro lint my-spec.yml

# Windows
polychro.exe lint my-spec.yml
```

### MCP Server

The same binary runs as an MCP server:

```bash
polychro serve --ruleset polychro:ai-safety
```

Add to your MCP client configuration:

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

### Java Library

For embedding in JVM applications (requires Java 21+, Maven 3.9+):

```xml
<dependency>
    <groupId>io.polychro</groupId>
    <artifactId>polychro-core</artifactId>
    <version>0.1.0</version>
</dependency>
```

For polyglot custom functions (requires GraalVM 21+):

```xml
<dependency>
    <groupId>io.polychro</groupId>
    <artifactId>polychro-ruleset-polyglot</artifactId>
    <version>0.1.0</version>
</dependency>
```

### SDK Clients (Go, Node.js, Python)

For non-JVM applications, install the SDK for your language. Each SDK is a thin wrapper that shells out to the native `polychro` binary (resolved via `POLYCHRO_BIN`, an adjacent `bin/` directory, or `PATH`).

**Go:**

```bash
go get github.com/naftiko/polychro/polychro-go
```

```go
import "github.com/naftiko/polychro/polychro-go"

result, err := polychro.Lint("my-spec.yml", polychro.Options{
    Ruleset: "polychro:ai-safety",
})
if result.HasErrors() {
    fmt.Println(result.ToAgentFormat())
}
```

**Node.js / TypeScript:**

```bash
npm install polychro
```

```ts
import { lint } from "polychro";

const result = await lint("my-spec.yml", { ruleset: "polychro:ai-safety" });
if (result.diagnostics.some(d => d.severity === "error")) {
    console.log(result.diagnostics);
}
```

**Python:**

```bash
pip install polychro
```

```python
from polychro import Linter

linter = Linter(ruleset="polychro:ai-safety")
result = linter.lint("my-spec.yml")
if result.has_errors:
    print(result.to_agent_format())
```

All three SDKs share the same surface: `lint`, `lintString` / `lint_string`, `validateSchema` / `validate_schema`, with typed `LintResult` and `Diagnostic` types.

### Build from Source

```bash
git clone https://github.com/naftiko/polychro.git
cd polychro
mvn clean install
```

For native compilation:

```bash
mvn -B clean package -Pnative -pl polychro-cli
```

## First Lint — CLI

```bash
# Lint with the default governance ruleset
polychro lint my-spec.yml

# Lint with a specific ruleset
polychro lint --ruleset polychro:ai-safety my-spec.yml

# Lint with agent-optimized output (JSON, token-counted)
polychro lint --format agent my-spec.yml
```

### Example Output

```
my-spec.yml
  2:1  warning  capability-description-present  Capability must include a description  
  1:1  warning  capability-version-format       Version must follow semver format       

✖ 2 problems (0 errors, 2 warnings)
```

## First Lint — Java API

> For JVM applications that need in-process linting. Most users should start with the CLI or MCP server above.

```java
import io.polychro.api.Document;
import io.polychro.api.Diagnostic;
import io.polychro.core.Linter;

import java.util.List;

public class QuickStart {
    public static void main(String[] args) {
        // Create a linter with default configuration
        Linter linter = Linter.builder().build();

        // Load a document
        Document doc = Document.fromString("""
            name: my-capability
            version: 1.0.0
            """, "yaml");

        // Lint and get diagnostics
        List<Diagnostic> issues = linter.lint(doc);

        // Print results
        issues.forEach(d -> System.out.printf("[%s] %s at %s%n",
            d.severity(), d.message(), d.range()));
    }
}
```

## Configuration File

Create a `.polychro.yml` at your project root:

```yaml
validators:
  wellformedness:
    maxDepth: 50
    maxKeyLength: 256
  json-schema:
    path: schemas/my-schema.json
  ruleset:
    path: rules/my-rules.yml
    functionsDir: rules/functions/
```

See [Guide ‐ Configuration](Guide-‐-Configuration) for the full reference.

## Running in CI with the GitHub Action

For GitHub-hosted projects, the `naftiko/polychro-action` composite action wraps the CLI with glob expansion, SARIF output, automatic upload to GitHub Code Scanning, and a configurable `fail-on` severity threshold:

```yaml
# .github/workflows/lint.yml
- uses: naftiko/polychro-action@v1
  with:
    files: '**/*.yml,**/*.yaml'
    fail-on: error
```

See [Guide ‐ GitHub Action](Guide-‐-GitHub-Action) for the full inputs/outputs reference and recipes (PR comments, Checkov in the same run, version pinning).

## Security scanning with Checkov

For infrastructure-as-code documents (Terraform, Kubernetes, CloudFormation, Dockerfile), enable the optional `polychro-checkov` validator to layer security findings on top of Polychro's structural checks:

```yaml
validators:
  checkov:
    enabled: true
```

Requires a local `checkov` install (`pip install checkov`). See [Guide ‐ Checkov](Guide-‐-Checkov) for framework auto-detection, skip lists, and graceful-degradation behaviour.

## Next Steps

- [Tutorial](Tutorial) — Walk through a complete linting workflow
- [Guide ‐ Rulesets](Guide-‐-Rulesets) — Understand built-in rulesets and write custom rules
- [Architecture](Architecture) — Learn how the pipeline is structured
