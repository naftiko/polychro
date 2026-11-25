# Track 2 — Agent Files — Known limitations (release)

This file records what is known-incomplete about the engine relative to what its own
documentation promises, discovered while authoring this track. Nothing below is fixed
in this pass — the point is an honest inventory, same spirit as
[Track 1's `LIMITATIONS.md`](../track-1-yaml/LIMITATIONS.md).

## 1. Markdown format-profile auto-detection is documented but not wired (engine bug)

`docs/guide/markdown.md` states the `skill` / `agents` / `instructions` profiles are
"detected automatically from the document's source filename" (`SKILL.md`, `AGENTS.md`,
`*.instructions.md` / `*.prompt.md`). In the actual engine, `FormatDetector.detect(...)`
and `MarkdownValidatorFactory#createWithAutoDetect(config, sourcePath)` implement this
correctly, but `Linter.Builder.createValidator()` (`polychro-core`) only ever calls the
generic `ValidatorFactory#create(ValidatorConfig)` interface method — confirmed by a
full-repo grep, zero call sites for `createWithAutoDetect` outside
`polychro-markdown` itself.

**Impact:** `polychro lint AGENTS.md` with no config lints it as the `generic` profile —
`agents-missing-section` / `agents-unexpected-frontmatter` never fire, regardless of the
filename. Every step in this track from Step 2 onward must set
`config.markdown.format` explicitly in a `.polychro.yml`.

**Architectural note for whoever fixes this:** it isn't a one-line wiring fix.
`createWithAutoDetect` takes a single `sourcePath` at validator-*construction* time, but
one `Linter` instance (and its validator list) is reused across every file passed to a
single `polychro lint` call. Auto-detection is inherently per-document, so a real fix
likely means moving detection into `MarkdownValidator#validate` (per document, using
`Document.sourcePath()`) rather than the factory.

## 2. The CLI/config examples in `docs/guide/markdown.md` don't run as written (docs bug)

Independent of §1, the guide's own example commands and config are wrong on their own
terms — even a hypothetical engine with auto-detection fixed would not accept them:

- `polychro lint --validator markdown=skill SKILL.md` — **fails immediately** with
  `Unknown option: '--validator'` (exit 2). The real flag is `--validators`/`-v`
  (plural), and it only accepts a comma-separated list of bare validator *names*; there
  is no `key=value` syntax anywhere in `polychro-cli` (confirmed by reading
  `LintCommand.java` and by running the command).
- The `.polychro.yml` example shows `validators:` as a **map**
  (`validators: { markdown: { format: skill } }`). `LinterConfig.parse()` requires
  `validators:` to be a **list** of validator names, with a separate `config:` map keyed
  by validator name (`config: { markdown: { format: skill } } }`) — the same shape
  Track 1's `.polychro.yml` already uses for `json-schema`. Passing the map form is not
  rejected — it's **silently ignored**: confirmed by running the exact documented YAML
  against a fixture with real `skill`-profile violations and getting byte-identical
  output to *no config file at all*. That's arguably worse than an error, since nothing
  signals the misconfiguration.

**Working invocation, verified in this track's `EXPECTED-OUTPUT.md`:**
```yaml
# .polychro.yml
validators:
  - markdown
config:
  markdown:
    format: skill   # or: agents / instructions
```
```bash
polychro lint --config .polychro.yml SKILL.md
```

## 3. `--config` and `--ruleset`/`--schema` CLI flags are mutually exclusive

`LintCommand.buildLinter()` uses the `--config` file's `config:` map *instead of*
`buildConfigFromFlags()` whenever `--config` is passed — the `--ruleset`/`--schema` CLI
flags are silently ignored in that case. To combine a custom ruleset with a markdown
`format` override (Steps 7–8), the ruleset path must be declared inside the
`.polychro.yml` itself, under `config.ruleset.rulesetPath` — not passed alongside via
`--ruleset` on the command line. This isn't necessarily wrong behavior, but it is
undocumented, and easy to trip over.

## 5. No public Shipyard pages yet as of this file

The runnable example files in this directory exist and are verified; the public
tutorial page on the Shipyard docs site is a separate step, not yet done as of writing
this file.

## 6. No CI harness yet

Same as Track 1 §2: there is no JUnit harness linting these examples and asserting
their diagnostics, and nothing is wired into `quality-gate.yml`. Until that exists,
drift between these fixtures and the engine is not caught automatically.
