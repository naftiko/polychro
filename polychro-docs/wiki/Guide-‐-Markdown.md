# Guide ‐ Markdown

The `polychro-markdown` module validates Markdown documents for structural, formatting, and link correctness. It parses the input with [CommonMark](https://github.com/commonmark/commonmark-java), projects it into the same `Document` model the rest of the pipeline uses, and emits the unified `Diagnostic` format — so Markdown diagnostics merge seamlessly with well-formedness, schema, ruleset, and HTML findings.

## When to Use Markdown Validation

- Documentation sites with heading hierarchy, internal anchors, and relative file references
- Skill / agent specification files (`SKILL.md`, `AGENTS.md`, `*.instructions.md`, `*.prompt.md`)
- READMEs and blueprints that ship alongside YAML / JSON specs and must stay link-consistent
- Catch-all hardening for any Markdown asset distributed in a multi-format repository

## Format Profiles

`polychro-markdown` ships four format profiles. The profile is detected automatically from the document's source filename, or can be forced via configuration. The same core rule set runs in every profile — the profile only adds format-specific checks (e.g. required frontmatter fields).

| Profile | Detection | Adds checks |
|---|---|---|
| `generic` (default) | Anything not matched below | Core rules only |
| `skill` | Filename equals `SKILL.md` | Required `name` / `description` frontmatter, version typing, top-level sections |
| `agents` | Filename equals `AGENTS.md` | No frontmatter expected; required guidance sections |
| `instructions` | Filename ends in `.instructions.md` or `.prompt.md` | Required non-empty `applyTo` frontmatter |

Unknown profile names fall back to `generic`.

## Configuration

```yaml
# .polychro.yml
validators:
  markdown:
    # Optional — auto-detected from filename when omitted.
    # One of: generic, skill, agents, instructions
    format: generic

    # Maximum line length (default 120). URL-only and table lines are exempt.
    lineLength: 120

    # Preferred unordered list marker (default "-")
    listMarker: "-"

    # Opt-in external link reachability probing (default false).
    # See "External Link Probing" below.
    checkExternalLinks: false
    externalLinkTimeout: 5000        # milliseconds
    externalLinkRateLimit: 10        # max concurrent requests
```

Or via the CLI on a single file:

```bash
polychro lint --validator markdown=skill SKILL.md
polychro lint --validator markdown=instructions code-review.instructions.md
polychro lint README.md                   # auto-detects as generic
```

The Markdown validator picks up any `.md` file declared in your lint targets — well-formedness runs first, then ruleset / schema validators where applicable, then `polychro-markdown`.

## Rule Categories

All Markdown rules share short, format-agnostic codes (no `md-` prefix in the rule code itself, but `source: "markdown"` on the diagnostic). They are organised into five categories.

### Structure

| Code | Severity | What it catches |
|---|---|---|
| `no-content` | ERROR | Document has no parseable text content |
| `frontmatter-parse-error` | ERROR | YAML frontmatter (between `---` fences) is malformed |
| `heading-hierarchy` | WARN | Heading jumps a level (e.g. `##` → `####`) |
| `duplicate-anchor` | WARN | Two headings produce the same slugified anchor |
| `broken-internal-link` | WARN | `[text](#anchor)` references a slug that doesn't exist in this document |

### Formatting

| Code | Severity | What it catches |
|---|---|---|
| `line-too-long` | WARN | Line exceeds `lineLength` (URL-only and table lines exempt) |
| `trailing-whitespace` | INFO | Line ends in space or tab |
| `inconsistent-list-marker` | INFO | Unordered list uses a marker other than `listMarker` |
| `code-block-no-language` | INFO | Fenced code block has no language annotation |
| `no-blank-line-before-heading` | INFO | Heading directly follows content with no blank line |

### Links

| Code | Severity | What it catches |
|---|---|---|
| `broken-relative-link` | WARN | `[text](path/to/file.md)` resolves to a file that does not exist on disk |
| `broken-relative-anchor` | WARN | `[text](other.md#anchor)` resolves but the anchor is not in the target file |
| `broken-external-link` | WARN | Opt-in: external URL returns 4xx/5xx (see [External Link Probing](#external-link-probing)) |
| `unreachable-external-link` | INFO | Opt-in: external URL times out or fails DNS resolution |

### Profile-specific — `skill`

| Code | Severity | What it catches |
|---|---|---|
| `skill-missing-frontmatter` | ERROR | `SKILL.md` has no YAML frontmatter |
| `skill-missing-field` | ERROR | Required frontmatter field missing (`name`, `description`) |
| `skill-empty-field` | ERROR | Required frontmatter field is present but empty |
| `skill-version-not-string` | WARN | `version` frontmatter is not quoted as a string |
| `skill-no-sections` | WARN | Document has no top-level `##` sections |

### Profile-specific — `agents` / `instructions`

| Code | Severity | Profile | What it catches |
|---|---|---|---|
| `agents-unexpected-frontmatter` | WARN | `agents` | `AGENTS.md` should not declare YAML frontmatter |
| `agents-missing-section` | WARN | `agents` | One of the expected guidance sections is missing |
| `instructions-missing-applyto` | WARN | `instructions` | Frontmatter has no `applyTo` key |
| `instructions-empty-applyto` | ERROR | `instructions` | `applyTo` is present but empty |

## Examples

### `generic` profile — README and docs

```bash
polychro lint docs/getting-started.md
```

Catches heading-level skips, duplicate slugs, broken `#anchor` links, broken relative file paths, missing code-block languages, and overlong lines.

### `skill` profile — agent skill specifications

```bash
polychro lint --validator markdown=skill SKILL.md
```

Adds frontmatter validation on top of the generic rules: `name` and `description` must exist and be non-empty, `version` (if present) must be a string, and at least one `##` section must be defined.

### `instructions` profile — VS Code instruction files

```bash
polychro lint --validator markdown=instructions code-review.instructions.md
```

Enforces a non-empty `applyTo` frontmatter key so the instruction file can actually be scoped to files in the workspace.

## External Link Probing

External link reachability is **disabled by default** so validation stays offline-deterministic. When enabled, the Markdown validator delegates to `polychro-format-common`'s `ExternalLinkProbe`, which:

- Issues an HTTP HEAD (falling back to GET) per unique URL
- Caches results in-memory across documents in the same lint run
- Rate-limits concurrent requests with `externalLinkRateLimit`
- Times out individual requests at `externalLinkTimeout` milliseconds

Enable it explicitly:

```yaml
validators:
  markdown:
    checkExternalLinks: true
    externalLinkTimeout: 5000
    externalLinkRateLimit: 10
```

Network failures produce `unreachable-external-link` (INFO) rather than `broken-external-link` (WARN) so transient outages don't fail your CI.

## Source Ranges

Every diagnostic carries a precise `SourceRange` pointing back to the offending node. CommonMark source positions are preserved through the projection step, so line/column references match the original file even after structural projection.

## Shared Format Utilities

`polychro-markdown` and `polychro-html` share their link- and anchor-related logic through `polychro-format-common`:

- `AnchorCollector` — gathers fragment-target anchors from any projected `Document`
- `LinkResolver` — resolves relative URLs against the document's source path
- `BrokenLocalReferenceRule` — common implementation behind `broken-relative-link` / `broken-relative-anchor`
- `DuplicateAnchorRule` — common implementation behind `duplicate-anchor`
- `ExternalLinkProbe` — opt-in network reachability check (off by default)

This means anchor and broken-link diagnostics behave identically whether the document is Markdown or HTML — see [Architecture › `polychro-format-common`](Architecture#polychro-format-common) for the design rationale.

## Programmatic Use

```java
import io.polychro.spi.Document;
import io.polychro.spi.Diagnostic;
import io.polychro.core.Linter;

Linter linter = Linter.builder()
    .validatorConfig("markdown", Map.of(
        "format", "skill",
        "lineLength", 100,
        "checkExternalLinks", false
    ))
    .build();

Document doc = Document.fromPath(Path.of("SKILL.md"));
List<Diagnostic> issues = linter.lint(doc);
```

The same configuration works through any SDK — Go, Node.js, or Python — because they all shell out to the same native binary.

## Next Steps

- [Guide ‐ HTML](Guide-‐-HTML) — Sibling format guide, sharing the same anchor / link utilities
- [Guide ‐ Configuration](Guide-‐-Configuration) — Full configuration reference for all validators
- [Writing a Validator Plugin](Writing-a-Validator-Plugin) — Add your own Markdown rules via the SPI
- [Architecture](Architecture) — How `polychro-markdown` and `polychro-format-common` fit into the pipeline
