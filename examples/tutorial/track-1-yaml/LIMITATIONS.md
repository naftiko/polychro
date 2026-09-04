# Track 1 — YAML / OpenAPI — Known limitations (release)

This track is **scaffolded, domain-aligned** (maritime), and its expected output is
**verified against the `beta5` engine** (see §1). This file records what is
known-incomplete — including the one remaining divergence between the engine and the
tutorial narrative — so the gaps can be closed in a later iteration and a reader is not
misled. The point is an honest inventory.

## 1. Expected output — VERIFIED against beta5

The diagnostics (`code`, `severity`, `message`, exit code) in
[`EXPECTED-OUTPUT.md`](./EXPECTED-OUTPUT.md) were originally **verified on 2026-06-26**
against a freshly built `polychro 1.0.0-beta1-SNAPSHOT` CLI, and **re-verified on
2026-09-03** against `polychro 1.0.0-beta5`. The table records the **real** engine output,
not a reading of the source. One step still diverges from the intended narrative (§5);
the Step 4 schema wiring that used to diverge is now **resolved** (§6).

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

## 5. `non-string-key` false positive on quoted HTTP status codes (engine)

Every step's output carries `WARN [non-string-key]: Non-string YAML key: 200`, even
though the response code is written **quoted** (`"200":`) — the idiomatic, valid OpenAPI
form for a string key. The well-formedness validator
(`polychro-wellformedness/.../WellformednessValidator.java`, `isNonStringKey`) tests the
field name with `Double.parseDouble(key)` and ignores whether the original scalar was
quoted, so any key that *looks* numeric is flagged regardless of quoting. Jackson's
`YAMLParser.currentName()` returns the text `200` with no quote-style information, so the
check cannot distinguish a genuine non-string key (`200:` unquoted) from a quoted string
key (`"200":`).

Impact on this track: the warning appears on all 7 steps and raises **Step 1** and
**Step 7** from the intended exit `0` to exit `1`. It is a **false positive in the
engine**, not a defect in the fixtures — the fixtures already quote their status codes.

**To close (engine work, out of scope for #49):** make `isNonStringKey` (or its caller)
honour the scalar's quote style — a quoted scalar key is always a string. This needs its
own issue, a unit test in `WellformednessValidatorTest`, and 100% coverage; it is **not**
done here. Until then, the tutorial narrative must mention the warning rather than promise
a clean Step 1/7.

## 6. ~~Step 4 `config.json-schema` block is not consumed~~ — RESOLVED in `1.0.0-beta5`

**Status: resolved.** Re-verified on 2026-09-03 against `polychro 1.0.0-beta5`:

```
$ polychro lint --config .polychro.yml step-4-openapi.yml
ERROR at $.info.version [type]: $.info.version: number found, string expected
WARN [non-string-key]: Non-string YAML key: 200

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
