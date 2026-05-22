# Guide ‐ GitHub Action

The official `naftiko/polychro-action` GitHub Action wraps the Polychro CLI
as a composite action with first-class GitHub integrations: glob expansion,
a configurable severity threshold, SARIF output, automatic upload to
GitHub Code Scanning, and a Markdown summary for PR comments.

The action source lives in [`polychro-github-action`](https://github.com/naftiko/polychro)
and the manifest at the repo root (`action.yml`).

## Quick start

```yaml
# .github/workflows/lint.yml
name: Polychro Lint
on: [push, pull_request]

jobs:
  lint:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      security-events: write    # required to upload SARIF
    steps:
      - uses: actions/checkout@v4
      - uses: naftiko/polychro-action@v1
        with:
          files: '**/*.yml,**/*.yaml,**/*.json'
          fail-on: error
```

That's the whole setup. The action will:

1. Resolve a Polychro binary for the runner's OS and architecture (cached
   under `~/.polychro/bin` keyed by version + os + arch).
2. Expand the glob patterns into a sorted, deduplicated file list.
3. Run `polychro lint --format sarif <files>`.
4. Compute an exit code based on `fail-on`.
5. Upload the SARIF file to GitHub Code Scanning (visible under the
   repo's **Security → Code scanning alerts** tab).

## Inputs

All inputs are declared in `action.yml`:

| Input     | Required | Default     | Description |
|-----------|----------|-------------|-------------|
| `files`   | ✅       | `**/*.yml`  | Glob pattern(s) for files to lint. Multiple patterns separated by commas or newlines. |
| `ruleset` |          | _none_      | Path to a custom ruleset file. Forwarded as `--ruleset`. |
| `schema`  |          | _none_      | Path to a custom schema file. Forwarded as `--schema`. |
| `config`  |          | _none_      | Path to `.polychro.yml`. Forwarded as `--config`. |
| `format`  |          | `sarif`     | Output format: `sarif`, `json`, `text`, `agent`. |
| `fail-on` |          | `error`     | Minimum severity to fail the action: `error`, `warn`, `info`, `hint`. |
| `version` |          | `latest`    | Polychro version to use (e.g. `0.1.0`). `latest` resolves the most recent GitHub release. |

### Glob expansion

`files` accepts standard glob patterns relative to the repository root.
Multiple patterns may be combined with commas or newlines:

```yaml
files: |
  apis/**/*.yml
  configs/**/*.json
  !**/node_modules/**
```

Internally, the `GlobExpansion` helper walks the workspace once and matches
each pattern against the relative path. Results are sorted and
deduplicated, so a file matching two patterns is only linted once.

### `fail-on` threshold

`FailOnThreshold` computes the action's exit code:

| `fail-on` value | Action fails if there is ≥ 1 diagnostic at … |
|-----------------|----------------------------------------------|
| `error`         | `ERROR`                                      |
| `warn`          | `ERROR` or `WARN`                            |
| `info`          | `ERROR`, `WARN`, or `INFO`                   |
| `hint`          | any diagnostic                               |

Setting `fail-on: warn` is the most common choice for teams gradually
adopting Polychro — errors block, warnings show up in Code Scanning, hints
and infos stay non-blocking.

## Outputs

| Output       | Description |
|--------------|-------------|
| `sarif-file` | Absolute path to the generated SARIF report inside the runner workspace. |
| `exit-code`  | `0` if no diagnostics met the threshold, `1` otherwise. |
| `summary`    | A Markdown one-liner suitable for posting as a PR comment. |

The summary line is produced by `FailOnThreshold.formatSummary()` and looks
like:

```
**Polychro Lint Results:** 3 issue(s) found — 1 error(s), 2 warning(s)
```

## Recipes

### Post a summary on every PR

```yaml
      - uses: naftiko/polychro-action@v1
        id: polychro
        with:
          files: 'apis/**/*.yml'
          fail-on: warn

      - name: Comment on PR
        if: github.event_name == 'pull_request' && always()
        env:
          SUMMARY: ${{ steps.polychro.outputs.summary }}
        uses: actions/github-script@v7
        with:
          script: |
            github.rest.issues.createComment({
              issue_number: context.issue.number,
              owner: context.repo.owner,
              repo: context.repo.repo,
              body: process.env.SUMMARY
            });
```

### Combine with Checkov

Polychro's [Checkov integration](Guide-‐-Checkov.md) runs in-process — you
do **not** need a separate action. Just install Checkov in the runner and
enable the validator in `.polychro.yml`:

```yaml
      - name: Install Checkov
        run: pip install checkov

      - uses: naftiko/polychro-action@v1
        with:
          files: 'infra/**/*.tf,k8s/**/*.yml'
          config: .polychro.yml
          fail-on: error
```

With `validators.checkov.enabled: true` in `.polychro.yml`, the same run
emits both structural and security findings into one SARIF file.

### Pin a specific Polychro version

```yaml
      - uses: naftiko/polychro-action@v1
        with:
          version: 0.1.0
          files: '**/*.yml'
```

Pinning is recommended for reproducible CI; `latest` is convenient for
prototypes but resolves a different binary as releases land.

### Skip SARIF upload

If you don't want findings to appear in GitHub Code Scanning (e.g. private
fork without `security-events: write`), use a non-SARIF format:

```yaml
      - uses: naftiko/polychro-action@v1
        with:
          files: '**/*.yml'
          format: text
          fail-on: error
```

The SARIF upload step is conditional on `format == 'sarif'`.

## Caching

The action caches the resolved Polychro binary under `~/.polychro/bin`
keyed by `polychro-${version}-${os}-${arch}`. On a warm cache, the
"Download Polychro binary" step is a no-op. The first run on a runner OS
takes a few extra seconds.

## See also

- [Getting Started](Getting-Started.md) — CLI usage outside CI
- [Guide ‐ Configuration](Guide-‐-Configuration.md) — `.polychro.yml` reference
- [Guide ‐ Checkov](Guide-‐-Checkov.md) — adding security findings to the same run
