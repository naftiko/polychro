# Track 1 — YAML / OpenAPI — Known limitations (release)

This track is **scaffolded, domain-aligned** (maritime), and its expected output is now
**verified against the `beta1` engine** (see §1). This file records what is
known-incomplete — including two real divergences between the engine and the tutorial
narrative — so the gaps can be closed in a later iteration and a reader is not misled.
Nothing below is fixed in this pass; the point is an honest inventory.

## 1. Expected output — now VERIFIED against beta1

The diagnostics (`code`, `severity`, `message`, exit code) in
[`EXPECTED-OUTPUT.md`](./EXPECTED-OUTPUT.md) were **verified on 2026-06-26** by running a
freshly built `polychro 1.0.0-beta1-SNAPSHOT` CLI over each example from this directory.
The table now records the **real** engine output, not a reading of the source. Two steps
diverge from the intended narrative; both are captured as known limitations §5 and §6
below — they are **documented, not fixed**, in this pass.

The earlier UNVERIFIED hypotheses that held (single-fire rules, message-override,
polyglot path resolution, exit-code mapping) were all confirmed; the two that did not
hold became §5 and §6.

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

## 6. Step 4 `config.json-schema` block is not consumed

Step 4 is meant to run the JSON-Schema stage via `--config .polychro.yml` (which carries a
`config.json-schema.schemaPath: openapi-schema.json` block) and surface a `type` ERROR at
`$.info.version`. Instead the CLI exits `2` with:

```
Error: JsonStructureValidatorFactory requires 'schemaNode', 'schemaPath', or an explicit 'mode' in config
```

So the schema-model stage is **mis-wired**: the `.polychro.yml` config shape does not match
what the active factory (`JsonStructureValidatorFactory`) expects, and the intended
JSON-Schema validation never runs. The `1.0`-parses-as-float defect is therefore **not**
exercised at all.

**To close (out of scope for this pass):** reconcile the `.polychro.yml` `config` shape with
the validator factory's expected keys (`schemaNode` / `schemaPath` / `mode`), confirm whether
Step 4 should target `json-schema` or `json-structure`, and verify the resulting diagnostic.
Documented here, **not fixed**.
