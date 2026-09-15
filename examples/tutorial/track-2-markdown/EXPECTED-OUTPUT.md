# Track 2 — Agent Files (Crewing the AI Deckhands) — Expected output (verified against beta3)

> **STATUS: VERIFIED against `polychro 1.0.0-beta3-SNAPSHOT`.** Every row below was
> produced by running the freshly built `beta3` CLI against the fixture in this
> directory on 2026-07-22. Nothing is "fixed" in this pass: the goal is an honest
> snapshot of what the engine actually emits.

All commands are run **from inside the step's own subfolder**
(`agents/`, `skills/`, `instructions/`) except Step 1, which is run from the track
root — each subfolder carries its own `.polychro.yml` because the markdown
validator's `format` applies to the **whole lint invocation**, not per file (see
[`LIMITATIONS.md`](./LIMITATIONS.md) §1).

The theme: Shipyard now crews AI agents alongside human sailors, and every agent
needs paperwork before it's allowed aboard — `AGENTS.md` is the ship's **Standing
Orders**, `SKILL.md` is an individual crew member's **Certificate of Competency**,
and `*.instructions.md` is **Pilotage Instructions** for a specific watch.

Exit-code contract (same as Track 1, confirmed by running): `0` = no actionable
diagnostics; `1` = warnings present (no errors); `2` = at least one ERROR.

| Step | Folder | Command | Exit | Diagnostic(s) — code / severity / gist |
|---|---|---|---|---|
| 1 | *(root)* | `polychro lint step-1-generic.md` | 1 | `heading-hierarchy` / WARN / "Heading level skipped: expected h2 or lower, found h3" |
| 2 | `agents/` | `polychro lint --config .polychro.yml step-2-agents.md` | 1 | `agents-missing-section` / WARN / "AGENTS.md missing recommended section: ## Contribution Workflow" |
| 3 | `agents/` | `polychro lint --config .polychro.yml step-3-agents.md` | 1 | `agents-unexpected-frontmatter` / WARN / "AGENTS.md should not have YAML frontmatter" |
| 4 | `skills/` | `polychro lint --config .polychro.yml step-4-skill.md` | 2 | `skill-missing-frontmatter` / ERROR / "SKILL.md requires YAML frontmatter with 'name' and 'description'" |
| 5 | `skills/` | `polychro lint --config .polychro.yml step-5-skill.md` | 2 | `skill-empty-field` / ERROR / "SKILL.md frontmatter field must not be empty: description" |
| 6 | `skills/` | `polychro lint --config .polychro.yml step-6-skill.md` | 1 | `skill-version-not-string` / WARN / "SKILL.md 'version' should be a quoted string (e.g. \"1.0\"), found: NUMBER" **+** `skill-no-sections` / WARN / "SKILL.md body should have at least one ## section" |
| 7 | `skills/` | `polychro lint --config .polychro-ruleset.yml step-7-skill.md` | 1 | `skill-certifying-officer-present` / WARN / "SKILL.md frontmatter must name a \`certifying_officer\` — the crew member who signed off this competency." (at `$.document.frontmatter.certifying_officer`) |
| 8 | `skills/` | `polychro lint --config .polychro-ruleset.yml step-8-skill.md` | 1 | `section-lean` / WARN / "Keep each section lean — no more than 30 words. A skill is a quick reference, not a novel." (at `$.document.blocks[0]`, the offending section's heading) |
| 9 | `instructions/` | `polychro lint --config .polychro.yml step-9-instructions.md` | 1 | `instructions-missing-applyto` / WARN / ".instructions.md frontmatter should include 'applyTo' pattern" |
| 10 | `instructions/` | `polychro lint --config .polychro.yml step-10-instructions.md` | 2 | `instructions-empty-applyto` / ERROR / "'applyTo' must not be empty" |

> **Steps 7–8 stderr noise.** Running the `section-lean` polyglot function (Step 8, and
> the `skill-ruleset.yml` load in Step 7) prints GraalVM/Truffle interpreter warnings to
> **stderr** — these are not diagnostics and do not affect the result or exit code, same
> as Track 1 Step 6.

## Engine facts confirmed by running (beta3, 2026-07-22)

- **Exit codes** behave exactly as `LintCommand#computeExitCode`: WARN-only → `1`, any
  ERROR → `2`.
- **Format profile selection is config-only.** Filename-based auto-detection
  (`SKILL.md`→`skill`, `AGENTS.md`→`agents`, `*.instructions.md`→`instructions`),
  though documented in `docs/guide/markdown.md`, is never invoked by `polychro-core` or
  `polychro-cli` — confirmed by full-repo grep (zero call sites for
  `createWithAutoDetect`). Every step from Step 2 onward requires an explicit
  `.polychro.yml` with `config.markdown.format` set. See [`LIMITATIONS.md`](./LIMITATIONS.md) §1.
- **One profile per lint invocation.** A single `.polychro.yml` applies its
  `markdown.format` to every file passed to that `polychro lint` call — mixing, e.g., an
  `agents`-profile file and a `skill`-profile file in one command misapplies the wrong
  profile to one of them (confirmed by running). Each step's fixture is linted alone, or
  alongside same-profile fixtures only.
- **Custom ruleset + markdown profile combine cleanly** in one `.polychro.yml`: the
  `ruleset` and `markdown` validators both read from the same `config:` map (Steps 7–8).
- **Polyglot functions on markdown documents** receive the projected `Document` model —
  `targetVal.document.frontmatter` and `targetVal.document.blocks[*]` (each block has a
  `type`: `heading` / `paragraph` / `list` / `code-block`, plus `text`/`level`/`items` as
  applicable) — the same shape used by `MarkdownProjector`. `path` segments in a
  function's returned results are plain array segments (`["document", "blocks", 0]`), not
  a JSONPath string — same contract as Track 1's `operation-id-unique.js`.
