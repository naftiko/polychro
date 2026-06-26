# Track 1 — YAML / OpenAPI — Known limitations (release)

This track is **scaffolded and domain-aligned** (maritime), but it has **not been
verified against the engine** yet. This file records what is known-incomplete so the
gaps can be closed in a later iteration, and so a reader is not misled.

## 1. Expected output is UNVERIFIED

Every diagnostic (`code`, `severity`, `message`, exit code) claimed in
[`EXPECTED-OUTPUT.md`](./EXPECTED-OUTPUT.md) is **derived from reading the engine
source**, not from running the linter. The only local CLI binary
(`target-test/polychro-cli-windows-amd64.exe`) is **`alpha3`** while the repo is
**`beta1`** — a stale binary whose output would not be authoritative — so the
`tuto_writer` auto-lint loop was deliberately skipped this round.

**To close:** run the JUnit harness (or a freshly built `beta1` CLI) over each
example, capture the real output, and correct any divergence in `EXPECTED-OUTPUT.md`.
The example files are the runnable truth; the table is a hypothesis until then.

Highest-risk rows to verify first (also listed in `EXPECTED-OUTPUT.md`):

- **Step 4** — the exact JSON-Schema diagnostic `code` (`type`?) and `path` format,
  and that Jackson YAML actually parses `info.version: 1.0` as a float (so the
  `type: string` constraint fails). If `1.0` is read as a string, Step 4 does **not**
  fail and the defect must be reworked.
- **Step 6** — the polyglot path resolution with a `/ships/{imo_number}` segment, and
  which message surfaces (the ruleset's `message` vs the function's own).
- **Steps 3 / 5** — the "exactly one rule fires" assumption.

## 2. No CI harness yet

There is no JUnit harness linting these examples and asserting their diagnostics, and
nothing is wired into `ci.yml`. Until that exists, a drift between the examples and the
engine is **not** caught automatically. (Harness wiring is the main agent's job, per the
`tuto_writer` boundaries — out of scope for the realignment.)

## 3. Step 5 does not yet realize the "casing" promise

The drydock describes Step 5 as the place where the contract's intentional
`snake_case` / `camelCase` mix (`imo_number` vs `crewIds`) becomes a teachable
**API-style / casing** defect. The current `openapi-ruleset.yml` has **no casing rule**:
Step 5 actually fires `operation-id-present` (a `truthy` check on a missing
`operationId`), not a casing rule.

The engine **does** ship a `casing` function (`CasingFunction.java`), but it is not
wired into the tutorial ruleset. Materializing the blueprint's casing promise is a
**content evolution** (add a `casing` rule on response fields), intentionally **not**
done during this domain realignment to avoid changing each step's diagnostic type.

## 4. No public Shipyard pages yet

The runnable example files exist, but the public tutorial pages
(`tutorials/track-1-yaml/`) on the Shipyard, the nav update, and the README link are
**not** done. This file covers the `polychro` examples only.
