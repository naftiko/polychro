# Guide ‐ HTML

The `polychro-html` module validates HTML documents and fragments against four built-in profiles. It parses the input with [JSoup](https://jsoup.org/), projects it into the same `Document` model the rest of the pipeline uses, and emits the unified `Diagnostic` format — so HTML diagnostics merge seamlessly with well-formedness, schema, ruleset, and Markdown findings.

## When to Use HTML Validation

- Email templates that must avoid `<script>` and inline event handlers
- Embedded UI fragments injected into trusted shells (no scripts, no inline styles)
- Documentation HTML where heading order, alt text, and broken anchors matter
- Catch-all hardening for any HTML asset shipped alongside a YAML / JSON spec

## Profiles

`polychro-html` ships with four profiles. Pick the profile that matches the document's deployment context — the same rule set runs in every profile, but the profile flips a few switches such as "are `<script>` and inline styles permitted?".

| Profile | Document structure | `<script>` allowed | Inline styles allowed | Parser mode | Typical use |
|---|---|---|---|---|---|
| `generic` (default) | Optional | Yes | Yes | Document | Catch-all permissive validation |
| `document` | **Required** (`<html>`, `<head>`, `<title>`, `lang`) | Yes | Yes | Document | Full standalone HTML pages |
| `fragment` | Optional | Yes | Yes | Fragment | Snippets injected into existing pages |
| `email` | Optional | **No** | Yes | Document | Email templates (no JS, inline styles required) |
| `embedded-ui` | Optional | **No** | **No** | Fragment | Trusted-shell UI components |

Unknown profile names fall back to `generic`.

## Configuration

```yaml
# .polychro.yml
validators:
  html:
    # One of: generic, document, fragment, email, embedded-ui
    profile: document
```

Or via the CLI on a single file:

```bash
polychro lint --validator html=document index.html
polychro lint --validator html=email welcome.html
polychro lint --validator html=embedded-ui card.html
```

The HTML validator picks up any `.html` file declared in your lint targets — well-formedness still runs first, then ruleset / schema validators where applicable, then `polychro-html`.

## Rule Categories

All HTML rules share the `html-` code prefix. They are organised into four categories, each emitted by a dedicated checker.

### Structure

| Code | Severity | What it catches | Profiles |
|---|---|---|---|
| `html-duplicate-id` | ERROR | Same `id` attribute used on two or more elements | All |
| `html-heading-order` | WARN | Heading jumps a level (e.g. `<h1>` → `<h3>`) | All |
| `html-broken-fragment` | WARN | `<a href="#x">` references an `id` / `<a name>` that doesn't exist | All |
| `html-missing-lang` | WARN | `<html>` is missing or has no `lang` attribute | `document` |
| `html-missing-title` | WARN | Document has no non-empty `<title>` | `document` |

### Accessibility

| Code | Severity | What it catches |
|---|---|---|
| `html-img-missing-alt` | WARN | `<img>` without an `alt` attribute |
| `html-form-field-unlabeled` | WARN | `<input>` / `<select>` / `<textarea>` (other than `hidden`, `submit`, `button`) with no `<label for="...">` |
| `html-table-missing-headers` | WARN | `<table>` with no `<th>` cells |

### Security

| Code | Severity | What it catches | Profiles |
|---|---|---|---|
| `html-inline-event-handler` | ERROR | `on*` attributes (e.g. `onclick`, `onerror`) | All |
| `html-javascript-url` | ERROR | `href`, `src`, or `action` starting with `javascript:` | All |
| `html-target-blank-noopener` | WARN | `target="_blank"` without `rel="noopener"` or `noreferrer` | All |
| `html-script-disallowed` | ERROR | `<script>` present | `email`, `embedded-ui` |
| `html-inline-style-disallowed` | WARN | `style="..."` attribute present | `embedded-ui` |

### Assets and links

| Code | Severity | What it catches |
|---|---|---|
| `html-missing-local-asset` | WARN | Relative `href` / `src` (on `<a>`, `<img>`, `<link>`, `<script>`) resolves to a path that does not exist on disk |

External URL probing is intentionally **not** performed by `polychro-html` — validation stays offline-deterministic by default. External link reachability is available as an opt-in extension through `polychro-format-common`'s `ExternalLinkProbe`, shared with the Markdown validator.

## Examples

### `document` profile — strict full-page validation

```bash
polychro lint --validator html=document landing.html
```

Catches missing `lang`, missing `<title>`, duplicate ids, heading order jumps, `javascript:` URLs, inline event handlers, and broken anchor fragments.

### `email` profile — no scripts, inline styles welcome

```bash
polychro lint --validator html=email welcome-email.html
```

Adds `html-script-disallowed` on top of the security baseline; allows `style="..."` because most email clients require it.

### `embedded-ui` profile — strict fragment

```bash
polychro lint --validator html=embedded-ui card-widget.html
```

Treats the input as a fragment (no `<html>` / `<head>` / `<body>` wrapper required), rejects both `<script>` and inline `style="..."`. Use this for UI fragments injected into a trusted shell where any inline script or style would bypass the host's CSP.

## Source Ranges

Every diagnostic carries a precise `SourceRange` pointing back to the offending element, because JSoup is configured with source-range tracking enabled. When elements are synthesized by the parser (rare — typically only the implicit `<html>` wrapper in fragment mode), Polychro falls back to `(1,1)-(1,1)`.

## Shared Format Utilities

`polychro-html` and `polychro-markdown` share their link- and anchor-related logic through `polychro-format-common`:

- `AnchorCollector` — gathers fragment-target anchors from any projected `Document`
- `LinkResolver` — resolves relative URLs against the document's source path
- `BrokenLocalReferenceRule` — emits `*-broken-local-reference` diagnostics across formats
- `DuplicateAnchorRule` — emits `*-duplicate-anchor` diagnostics across formats
- `ExternalLinkProbe` — opt-in network reachability check (off by default)

This means anchor and broken-link diagnostics behave identically whether the document is Markdown or HTML.

## Programmatic Use

```java
import io.polychro.spi.Document;
import io.polychro.spi.Diagnostic;
import io.polychro.core.Linter;

Linter linter = Linter.builder()
    .validatorConfig("html", Map.of("profile", "email"))
    .build();

Document doc = Document.fromPath(Path.of("welcome-email.html"));
List<Diagnostic> issues = linter.lint(doc);
```

The same configuration works through any SDK — Go, Node.js, or Python — because they all shell out to the same native binary.

## Next Steps

- [Guide ‐ Configuration](Guide-‐-Configuration) — Full configuration reference for all validators
- [Writing a Validator Plugin](Writing-a-Validator-Plugin) — Add your own HTML rules via the SPI
- [Architecture](Architecture) — How `polychro-html` and `polychro-format-common` fit into the pipeline
