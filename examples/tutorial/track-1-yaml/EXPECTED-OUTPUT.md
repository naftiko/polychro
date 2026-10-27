# Track 1 — YAML / OpenAPI — Expected output (verified against beta9-SNAPSHOT)

> **STATUS: VERIFIED against `polychro 1.0.0-beta9-SNAPSHOT`.** Every row below was
> re-verified on 2026-10-27 by running a CLI built from `main` from this directory
> (previously verified against `1.0.0-beta5` on 2026-09-03 and `1.0.0-beta1-SNAPSHOT`
> on 2026-06-26). Every step now behaves as the tutorial narrative intends: the
> `non-string-key` false positive on quoted status codes is **resolved** (see
> [`LIMITATIONS.md`](./LIMITATIONS.md), point 5).

All commands are run **from** `examples/tutorial/track-1-yaml/` so the relative
paths (`openapi-ruleset.yml`, `openapi-schema.json`, `.polychro.yml`,
`functions/operation-id-unique.js`) resolve.

The contract is the **Modern Maritime Registry API** slice scoped to three
read-only operations (`GET /ships` → `list-ships`, `GET /ships/{imo_number}` →
`get-ship`, `GET /crew` → `list-crew`), per the drydock. Because the track is
read-only, each step's defect is carried by a **GET** operation.

Exit-code contract (verified by reading `polychro-cli/.../LintCommand.java#computeExitCode`
and confirmed by running): `0` = no actionable diagnostics (empty, or INFO/HINT only);
`1` = warnings present (no errors); `2` = at least one ERROR (also file-not-found /
parse failure / validator-config error).

| Step | Command | Exit | Diagnostic(s) — code / severity / gist |
|---|---|---|---|
| 1 | `polychro lint step-1-openapi.yml` | 0 | None — "No issues found." The quoted `"200"` response code is a string key and is no longer flagged (this was LIMITATIONS §5, **resolved**). |
| 2 | `polychro lint step-2-openapi.yml` | 2 | `duplicate-key` / ERROR / "Duplicate key: summary" (duplicated `summary` on the `/ships` path item) |
| 3 | `polychro lint --ruleset openapi-ruleset.yml step-3-openapi.yml` | 1 | `path-summary-present` / WARN / "Each path item must declare a `summary`." (at `$.paths./ships/{imo_number}`) |
| 4 | `polychro lint --config .polychro.yml step-4-openapi.yml` | 2 | `ERROR at $.info.version [type]: $.info.version: number found, string expected`. The `config.json-schema` block in `.polychro.yml` is consumed and the JSON-Schema stage runs as the narrative intends (this was LIMITATIONS §6, **resolved**). |
| 5 | `polychro lint --ruleset openapi-ruleset.yml step-5-openapi.yml` | 2 | `operation-id-present` / ERROR / "Each operation must declare an `operationId`." (at `$.paths./crew.get`) |
| 6 | `polychro lint --ruleset openapi-ruleset.yml step-6-openapi.yml` | 2 | `operation-id-unique` / ERROR / "Every `operationId` must be unique across the whole contract." (at `$.paths./ships/{imo_number}.get.operationId`; the ruleset's `message` overrides the function's own, as predicted). GraalVM/Truffle warnings are printed on stderr when the polyglot function runs. |
| 7 | `polychro lint --format agent step-7-openapi.yml` | 0 | AgentFormatter JSON with no diagnostics: `{"diagnostics":[],"summary":{"errors":0,"warnings":0,"info":0},"tokens":27}` (pretty-printed by the CLI). |

> **Step 7 fixture:** `step-7-openapi.yml` is byte-identical to `step-1-openapi.yml` — the teaching point of Step 7 is the `--format agent` flag, not a new defect in the contract.

## Engine facts confirmed by running (beta1 2026-06-26, beta5 2026-09-03, beta9-SNAPSHOT 2026-10-27)

- **Exit codes** behave exactly as `LintCommand#computeExitCode` reads: WARN-only → `1`,
  any ERROR → `2`, validator-config error → `2`.
- **`duplicate-key`** (Step 2) fires as an ERROR on a duplicated mapping key, as expected.
- **Ruleset rules** `path-summary-present` (Step 3, WARN), `operation-id-present`
  (Step 5, ERROR) and `operation-id-unique` (Step 6, ERROR) each fire **once**, with the
  diagnostic `path` in `$.paths.<path>.…` form and the rule's own `message` — confirming
  the single-fire design and the message-override (ruleset `message` wins over the
  polyglot function's message).
- **Polyglot** path resolution handles the `/ships/{imo_number}` segment without throwing;
  GraalVM/Truffle prints interpreter warnings to **stderr** (not part of the diagnostics).
- **No divergence from the intended narrative** remains. The `non-string-key` false
  positive on quoted status codes (formerly [`LIMITATIONS.md`](./LIMITATIONS.md) §5) and
  the Step 4 `config.json-schema` block (formerly §6) are both **resolved**.
