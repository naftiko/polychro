# Writing a Validator Plugin

Polychro's SPI allows you to add custom validators without modifying the core library. This guide walks through creating, packaging, and registering a custom validator.

## Step 1 — Add the API Dependency

```xml
<dependency>
    <groupId>io.polychro</groupId>
    <artifactId>polychro-api</artifactId>
    <version>0.1.0</version>
</dependency>
```

## Step 2 — Implement ValidatorFactory

```java
package com.example.polychro;

import io.polychro.api.Validator;
import io.polychro.api.ValidatorConfig;
import io.polychro.api.ValidatorFactory;

public class MyValidatorFactory implements ValidatorFactory {

    @Override
    public String name() {
        return "my-validator";
    }

    @Override
    public Validator create(ValidatorConfig config) {
        return new MyValidator(config);
    }
}
```

## Step 3 — Implement Validator

```java
package com.example.polychro;

import io.polychro.api.Diagnostic;
import io.polychro.api.Document;
import io.polychro.api.Range;
import io.polychro.api.Severity;
import io.polychro.api.Validator;
import io.polychro.api.ValidatorConfig;

import java.util.ArrayList;
import java.util.List;

public class MyValidator implements Validator {

    private final ValidatorConfig config;

    MyValidator(ValidatorConfig config) {
        this.config = config;
    }

    @Override
    public String name() {
        return "my-validator";
    }

    @Override
    public int priority() {
        // Run after schema (200) but before ruleset (300)
        return 250;
    }

    @Override
    public List<Diagnostic> validate(Document document, ValidatorConfig config) {
        List<Diagnostic> diagnostics = new ArrayList<>();

        // Your validation logic here
        String content = document.content();

        if (!content.contains("version:")) {
            diagnostics.add(new Diagnostic(
                "my-version-required",
                Severity.ERROR,
                "Document must declare a version",
                Range.at(1, 1),
                name(),
                "Add a 'version' field at the root level"
            ));
        }

        return diagnostics;
    }
}
```

## Step 4 — Register via ServiceLoader

Create the file `META-INF/services/io.polychro.api.ValidatorFactory`:

```
com.example.polychro.MyValidatorFactory
```

In Maven, place this at:
```
src/main/resources/META-INF/services/io.polychro.api.ValidatorFactory
```

## Step 5 — Package and Use

```bash
mvn clean package
```

Add your validator JAR to the classpath alongside `polychro-core`, and it will be discovered automatically:

```java
// Your validator participates without any explicit registration
Linter linter = Linter.builder()
    .config(Path.of(".polychro.yml"))
    .build();

List<Diagnostic> issues = linter.lint(doc);
// Includes diagnostics from your validator
```

## Configuration

Your validator can read configuration from `.polychro.yml`:

```yaml
validators:
  my-validator:
    customOption: value
    threshold: 10
```

Access it in your validator:

```java
@Override
public List<Diagnostic> validate(Document document, ValidatorConfig config) {
    String customOption = config.getString("customOption", "default");
    int threshold = config.getInt("threshold", 5);
    // ...
}
```

## Priority Ordering

Built-in validator priorities:

| Validator | Priority | Rationale |
|---|---|---|
| Well-formedness | 100 | Must run first — structural issues invalidate everything else |
| Schema | 200 | Shape validation before semantic checks |
| JSON Structure | 250 | Strict typing after schema |
| Ruleset | 300 | Semantic rules assume valid structure |
| Markdown | 400 | Document-level checks run last |

Choose a priority that positions your validator correctly in the pipeline.

## Testing

Test your validator independently using the Polychro API:

```java
@Test
void shouldDetectMissingVersion() {
    MyValidator validator = new MyValidator(ValidatorConfig.empty());
    Document doc = Document.fromString("name: test\n", "yaml");

    List<Diagnostic> diagnostics = validator.validate(doc, ValidatorConfig.empty());

    assertThat(diagnostics).hasSize(1);
    assertThat(diagnostics.get(0).code()).isEqualTo("my-version-required");
}
```

Or test through the full pipeline:

```java
@Test
void shouldRunInPipeline() {
    Linter linter = Linter.builder().build();
    Document doc = Document.fromString("name: test\n", "yaml");

    List<Diagnostic> diagnostics = linter.lint(doc);

    assertThat(diagnostics)
        .anyMatch(d -> d.code().equals("my-version-required"));
}
```
