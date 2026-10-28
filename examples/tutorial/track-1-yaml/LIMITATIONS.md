# Track 1 — YAML / OpenAPI — Known limitations (release)

This track is **scaffolded, domain-aligned** (maritime), and its expected output is
**verified against the `beta9-SNAPSHOT` engine** (see §1). This file records what is
known-incomplete — and the engine divergences that have since been resolved — so the gaps
can be closed in a later iteration and a reader is not misled. The point is an honest inventory.

## 1. Expected output — VERIFIED against beta9-SNAPSHOT

The diagnostics (`code`, `severity`, `message`, exit code) in
[`EXPECTED-OUTPUT.md`](./EXPECTED-OUTPUT.md) were originally **verified on 2026-06-26**
against a freshly built `polychro 1.0.0-beta1-SNAPSHOT` CLI, and **re-verified on
2026-09-03** against `polychro 1.0.0-beta5` and on **2026-10-27** against a CLI built
from the [#218](https://github.com/naftiko/polychro/pull/218) branch (`1.0.0-beta9-SNAPSHOT`). The table records the **real** engine output, not a
reading of the source. No step diverges from the intended narrative any more: the
`non-string-key` false positive (§5) and the Step 4 schema wiring (§6) are both
**resolved**.

The earlier UNVERIFIED hypotheses that held (single-fire rules, message-override,
polyglot path resolution, exit-code mapping) were all confirmed.

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
wired into the tutorial ruleset. Materializing the design doc's casing promise is a
**content evolution** (add a `casing` rule on response fields), intentionally **not**
done during this domain realignment to avoid changing each step's diagnostic type.

## 4. No public Shipyard pages yet

The runnable example files exist, but the public tutorial pages
(`tutorials/track-1-yaml/`) on the Shipyard, the nav update, and the README link are
**not** done. This file covers the `polychro` examples only.

## 5. ~~`non-string-key` false positive on quoted HTTP status codes~~ — RESOLVED ([#78](https://github.com/naftiko/polychro/issues/78))

**Status: resolved.** Re-verified on 2026-10-27 against a CLI built from the [#218](https://github.com/naftiko/polychro/pull/218) branch
(`1.0.0-beta9-SNAPSHOT`): Steps 1 and 7 exit `0` with no diagnostics, and no step carries
the `non-string-key` WARN any more.

The well-formedness validator now walks the raw SnakeYAML event stream instead of Jackson
tokens, so it sees each key's scalar style: only a **plain** (unquoted, not tagged `!!str`) key that
looks like a number, boolean or null is flagged. `"200":`, `'200':` and `!!str 200:` are
string keys and stay silent; an unquoted `200:` is still reported.

**Historical record (what this section used to describe).** Every step's output carried
`WARN [non-string-key]: Non-string YAML key: 200` even though the fixtures quote their
response codes, because Jackson's `YAMLParser.currentName()` drops quote-style
information. This raised Step 1 and Step 7 from the intended exit `0` to exit `1`.

## 6. ~~Step 4 `config.json-schema` block is not consumed~~ — RESOLVED in `1.0.0-beta5`

**Status: resolved.** Re-verified on 2026-09-03 against `polychro 1.0.0-beta5`:

```
$ polychro lint --config .polychro.yml step-4-openapi.yml
ERROR at $.info.version [type]: $.info.version: number found, string expected
WARN [non-string-key]: Non-string YAML key: 200   # gone since §5 was resolved

2 issue(s) found.   # exit 2
```

Step 4 now runs the JSON-Schema stage via `--config .polychro.yml` (which carries a
`config.json-schema.schemaPath: openapi-schema.json` block) and surfaces the `type` ERROR
at `$.info.version` — the `1.0`-parses-as-float defect is exercised exactly as the
narrative intends, so no fixture change is needed.

**Historical record (what this section used to describe).** The CLI previously exited `2`
with `Error: JsonStructureValidatorFactory requires 'schemaNode', 'schemaPath', or an
explicit 'mode' in config`: the `.polychro.yml` config shape did not match what the active
factory expected, and the intended JSON-Schema validation never ran.
