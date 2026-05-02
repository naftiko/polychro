# Getting Started

## Prerequisites

- **Java 21+** — required for all usage modes
- **Maven 3.9+** — for building from source or using as a library dependency
- **GraalVM 21+** — optional, needed for polyglot custom functions (JS, Python, Groovy) and native compilation

## Installation

### Maven Dependency (Library)

Add the core module to your project:

```xml
<dependency>
    <groupId>io.polychro</groupId>
    <artifactId>polychro-core</artifactId>
    <version>0.1.0</version>
</dependency>
```

This includes all built-in validators. For polyglot custom functions, add:

```xml
<dependency>
    <groupId>io.polychro</groupId>
    <artifactId>polychro-ruleset-polyglot</artifactId>
    <version>0.1.0</version>
</dependency>
```

### CLI (Native Binary)

Download the native binary for your platform from the [Releases](https://github.com/naftiko/polychro/releases) page:

```bash
# Linux / macOS
chmod +x polychro
./polychro lint my-spec.yml

# Windows
polychro.exe lint my-spec.yml
```

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

## First Lint — Java API

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

## Configuration File

Create a `.polychro.yml` at your project root:

```yaml
validators:
  wellformedness:
    maxDepth: 50
    maxKeyLength: 256
  schema:
    path: src/main/resources/schemas/my-schema.json
  ruleset:
    path: src/main/resources/rules/my-rules.yml
    functionsDir: src/main/resources/rules/functions/
```

See [[Guide ‐ Configuration]] for the full reference.

## Next Steps

- [[Tutorial]] — Walk through a complete linting workflow
- [[Guide ‐ Rulesets]] — Understand built-in rulesets and write custom rules
- [[Architecture]] — Learn how the pipeline is structured
