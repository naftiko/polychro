# Track 1 — YAML / OpenAPI — Expected output (verified against beta1)

> **STATUS: VERIFIED against `polychro 1.0.0-beta1-SNAPSHOT`.** Every row below was
> produced by running the freshly built `beta1` CLI from this directory on
> 2026-06-26. Two steps do **not** yet behave as the tutorial narrative intends —
> they are recorded here faithfully and tracked as **known limitations** (see
> [`LIMITATIONS.md`](./LIMITATIONS.md), points 5 and 6). Nothing is "fixed" in this
> pass: the goal is an honest snapshot of what the engine actually emits.

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

> **Caveat carried by every step — `non-string-key` on quoted HTTP status codes.**
> The well-formedness validator emits `WARN [non-string-key]: Non-string YAML key: 200`
> on every file, because the HTTP response code `"200"` — although quoted, and a valid
> idiomatic OpenAPI string key — is reported as non-string. This is a **false positive**
> in the engine (see [`LIMITATIONS.md`](./LIMITATIONS.md) point 5), not a defect in the
> fixtures. It raises Step 1 and Step 7 from the intended exit `0` to exit `1`. The
> diagnostic is listed in every row below so the table matches reality.

| Step | Command | Exit | Diagnostic(s) — code / severity / gist |
|---|---|---|---|
| 1 | `polychro lint step-1-openapi.yml` | 1 | `non-string-key` / WARN / "Non-string YAML key: 200" (the only diagnostic — well-formedness; the quoted `"200"` response code is wrongly flagged, see LIMITATIONS §5). Intended: exit 0, clean. |
| 2 | `polychro lint step-2-openapi.yml` | 2 | `duplicate-key` / ERROR / "Duplicate key: summary" (duplicated `summary` on the `/ships` path item) **+** `non-string-key` / WARN / "Non-string YAML key: 200" |
| 3 | `polychro lint --ruleset openapi-ruleset.yml step-3-openapi.yml` | 1 | `path-summary-present` / WARN / "Each path item must declare a `summary`." (at `$.paths./ships/{imo_number}.summary`) **+** `non-string-key` / WARN / "Non-string YAML key: 200" |
| 4 | `polychro lint --config .polychro.yml step-4-openapi.yml` | 2 | `Error: JsonStructureValidatorFactory requires 'schemaNode', 'schemaPath', or an explicit 'mode' in config` — the `config.json-schema` block in `.polychro.yml` is **not** consumed as intended; the JSON-Schema stage never runs. Intended: a JSON-Schema `type` ERROR at `$.info.version`. See LIMITATIONS §6. |
| 5 | `polychro lint --ruleset openapi-ruleset.yml step-5-openapi.yml` | 2 | `operation-id-present` / ERROR / "Each operation must declare an `operationId`." (at `$.paths./crew.get.operationId`) **+** `non-string-key` / WARN / "Non-string YAML key: 200" |
| 6 | `polychro lint --ruleset openapi-ruleset.yml step-6-openapi.yml` | 2 | `operation-id-unique` / ERROR / "Every `operationId` must be unique across the whole contract." (at `$.paths./ships/{imo_number}.get.operationId`; the ruleset's `message` overrides the function's own, as predicted) **+** `non-string-key` / WARN / "Non-string YAML key: 200". GraalVM/Truffle warnings are printed on stderr when the polyglot function runs. |
| 7 | `polychro lint --format agent step-7-openapi.yml` | 1 | AgentFormatter JSON with one warning: `{"diagnostics":[{"severity":"warning","rule":"non-string-key","message":"Non-string YAML key: 200"}],"summary":{"errors":0,"warnings":1,"info":0},"tokens":55}`. Intended: clean (exit 0), but the `non-string-key` false positive applies here too. |

## Engine facts confirmed by running (beta1, 2026-06-26)

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
- **Two divergences from the intended narrative** are now known limitations, not engine
  facts to rely on: the `non-string-key` false positive on quoted status codes
  ([`LIMITATIONS.md`](./LIMITATIONS.md) §5), and the unconsumed `config.json-schema` block
  in Step 4 ([`LIMITATIONS.md`](./LIMITATIONS.md) §6). Neither is fixed in this pass.
