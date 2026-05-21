# Guide ‐ Checkov

Polychro can run [Checkov](https://www.checkov.io/) as an additional validator
to surface security and compliance issues in infrastructure-as-code documents
(Terraform, Kubernetes manifests, CloudFormation templates, Dockerfiles, and
generic YAML) alongside Polychro's own well-formedness, schema, and ruleset
checks.

Checkov support lives in the optional `polychro-checkov` module. It wraps a
locally installed `checkov` binary as a subprocess, parses its JSON output,
and surfaces results as standard Polychro `Diagnostic` objects — so a Checkov
finding looks the same as any other Polychro issue in your terminal, SARIF
report, or MCP feed.

> Polychro's ruleset engine focuses on **structural linting and governance**
> (shape, naming, organisational conventions). Checkov focuses on
> **security and compliance posture** (e.g. "this S3 bucket is public",
> "this Kubernetes Pod runs as root"). The two are complementary — running
> both is recommended for production CI pipelines.

## Prerequisites

Checkov is a Python tool. Polychro does **not** bundle it; you install it
once on the machine where Polychro runs:

```bash
pip install checkov
# or
pipx install checkov
```

Verify it's on PATH:

```bash
checkov --version
```

If `checkov` is not on PATH when Polychro runs, the `checkov` validator
emits a single `INFO` diagnostic (`checkov-not-installed`) and continues
gracefully — your build is not broken just because Checkov is missing.

## Enabling Checkov

Add the `checkov` validator block to your `.polychro.yml`:

```yaml
validators:
  checkov:
    enabled: true
    checkovPath: checkov          # optional, defaults to 'checkov' on PATH
    timeout: 60                   # seconds; default 60
    skipChecks:                   # optional Checkov check IDs to skip
      - CKV_AWS_18
      - CKV_K8S_8
    customCheckDir: ./policies    # optional path to extra Checkov checks
    framework: terraform          # optional manual override
```

All keys are optional except `enabled`. With just `enabled: true`, Checkov
runs with its full default check set and auto-detected framework.

### Configuration keys

| Key              | Type     | Default     | Description |
|------------------|----------|-------------|-------------|
| `enabled`        | boolean  | `false`     | Turn the validator on. |
| `checkovPath`    | string   | `checkov`   | Path or command name of the Checkov binary. |
| `timeout`        | integer  | `60`        | Subprocess timeout in seconds. After this Polychro kills the process and emits a `checkov-execution-error` diagnostic. |
| `skipChecks`     | string[] | `[]`        | Checkov check IDs to suppress (`--skip-check`). |
| `customCheckDir` | string   | _none_      | Directory with custom Checkov policies (`--external-checks-dir`). |
| `framework`      | string   | _auto_      | Force a specific framework (see table below). Bypasses auto-detection. |

## Supported frameworks

Polychro asks Checkov to scan against a single framework at a time
(`--framework <value>`). The framework is auto-detected from filename and
content unless overridden:

| Framework        | Detection trigger |
|------------------|-------------------|
| `terraform`      | Filename ends in `.tf` or `.tf.json` |
| `dockerfile`     | Filename is `Dockerfile` or starts with `Dockerfile.` |
| `kubernetes`     | `.yml` / `.yaml` file containing `apiVersion:` |
| `cloudformation` | `.yml` / `.yaml` file containing `AWSTemplateFormatVersion` |
| `yaml`           | Default fallback for any other YAML file |

If you have a YAML file that Checkov should always treat as, say, Kubernetes
(maybe it ships without the usual marker), set
`framework: kubernetes` in your config to skip auto-detection.

## Severity mapping

Checkov's severity vocabulary maps onto Polychro's `Severity` levels:

| Checkov severity   | Polychro severity |
|--------------------|-------------------|
| `CRITICAL`, `HIGH` | `ERROR`           |
| `MEDIUM`           | `WARN`            |
| `LOW`              | `INFO`            |
| _(unset)_          | `WARN`            |

Diagnostic codes are the raw Checkov check IDs (e.g. `CKV_AWS_18`), and the
message includes the guideline URL when Checkov reports one — so jumping
from a Polychro diagnostic to Bridgecrew's remediation docs is one click.

## Examples

### Terraform module

```bash
polychro lint main.tf --config .polychro.yml
```

With `validators.checkov.enabled: true`, Polychro will:

1. Run its standard YAML/JSON well-formedness checks (none apply to `.tf`).
2. Detect framework `terraform` from the extension.
3. Shell out to `checkov --file main.tf --framework terraform --output json --compact`.
4. Parse the JSON, keep only `FAILED` results, and map them to diagnostics.

### Kubernetes manifest with skip list

```yaml
# .polychro.yml
validators:
  wellformedness:
    enabled: true
  ruleset:
    enabled: true
    rules: ./team-conventions.yml
  checkov:
    enabled: true
    framework: kubernetes
    skipChecks:
      - CKV_K8S_8     # we accept liveness probes being optional
      - CKV_K8S_21    # we run in a controlled namespace
```

### Custom policies

If your team maintains in-house Checkov policies in `./security-policies`:

```yaml
validators:
  checkov:
    enabled: true
    customCheckDir: ./security-policies
```

Polychro passes the directory through with `--external-checks-dir`; Checkov
loads everything in it on top of its default rules.

## Graceful degradation

Two conditions are explicitly tolerated:

- **Checkov not installed.** A single `INFO` diagnostic
  `checkov-not-installed` is emitted; nothing fails the build. This makes
  it safe to commit `.polychro.yml` with Checkov enabled even when some
  developer machines don't have Python.
- **Document has no file path** (e.g. in-memory MCP usage). A single
  `INFO` diagnostic `checkov-no-file` is emitted instead of attempting to
  scan a synthetic path.

For all other failures (timeout, non-zero exit unrelated to findings,
malformed JSON), Polychro emits an `ERROR` diagnostic
`checkov-execution-error` with the underlying message.

## Programmatic use

```java
ValidatorConfig cfg = ValidatorConfig.of(Map.of(
    "checkovPath", "/usr/local/bin/checkov",
    "timeout", 90,
    "framework", "terraform",
    "skipChecks", List.of("CKV_AWS_18")
));

Validator checkov = new CheckovValidatorFactory().create(cfg);
List<Diagnostic> findings = checkov.validate(document);
```

The validator implements the standard `io.polychro.spi.Validator` SPI, so it
plugs into the same pipeline as every built-in validator.

## See also

- [Guide ‐ Configuration](Guide-‐-Configuration.md) — full `.polychro.yml` reference
- [Guide ‐ GitHub Action](Guide-‐-GitHub-Action.md) — running Checkov in CI
- [Architecture](Architecture.md) — the external-process validator pattern
