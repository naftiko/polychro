# Track 1 — YAML / OpenAPI — Claimed expected output (UNVERIFIED)

> **STATUS: UNVERIFIED.** The author (the `tuto_writer` agent) did NOT run the
> Polychro CLI for this realignment. The only local binary
> (`target-test/polychro-cli-windows-amd64.exe`) is `alpha3` while the repo is
> `beta1`, so its output would not be authoritative — the auto-lint loop was
> deliberately skipped this round. Every "expected" value below is derived from
> **reading the engine source** and from the prior `/users` baseline, not from
> running it. The main agent must verify each row against a JUnit harness (or a
> freshly built `beta1` CLI) and correct any divergence — the example files are
> the runnable truth; this table is a hypothesis.

All commands are run **from** `examples/tutorial/track-1-yaml/` so the relative
paths (`openapi-ruleset.yml`, `openapi-schema.json`, `.polychro.yml`,
`functions/operation-id-unique.js`) resolve.

The contract is the **Modern Maritime Registry API** slice scoped to three
read-only operations (`GET /ships` → `list-ships`, `GET /ships/{imo_number}` →
`get-ship`, `GET /crew` → `list-crew`), per the drydock. Because the track is
read-only, each step's defect is now carried by a **GET** operation (the old
`/users` files used `post` operations that do not exist in this contract).

Exit-code contract (verified by reading `polychro-cli/.../LintCommand.java#computeExitCode`):
`0` = no actionable diagnostics (empty, or INFO/HINT only); `1` = warnings present
(no errors); `2` = at least one ERROR (also file-not-found / parse failure).

| Step | Command | Expected exit | Expected diagnostic(s) — code / severity / gist | Status |
|---|---|---|---|---|
| 1 | `polychro lint step-1-openapi.yml` | 0 | none — `No issues found.` (well-formedness only; ruleset & schema factories are unconfigured and silently skipped) | UNVERIFIED |
| 2 | `polychro lint step-2-openapi.yml` | 2 | `duplicate-key` / ERROR / "Duplicate key: summary" (well-formedness; duplicated `summary` on the `/ships` path item) | UNVERIFIED |
| 3 | `polychro lint --ruleset openapi-ruleset.yml step-3-openapi.yml` | 1 | `path-summary-present` / WARN / "Each path item must declare a `summary`." (the `/ships/{imo_number}` path item has no summary; rules requiring operationId/responses stay green) | UNVERIFIED |
| 4 | `polychro lint --config .polychro.yml step-4-openapi.yml` | 2 | a JSON-Schema `type` ERROR at `$.info.version` (the schema requires `version` to be a string; `1.0` parses as a YAML float). The exact `code` is the networknt validator's `msg.getType()` — likely `type` — and the path format is `$.info.version`. | UNVERIFIED |
| 5 | `polychro lint --ruleset openapi-ruleset.yml step-5-openapi.yml` | 2 | `operation-id-present` / ERROR / "Each operation must declare an `operationId`." (the `get` on `/crew` has no `operationId`; `path-summary-present` and `operation-responses-present` stay green) | UNVERIFIED |
| 6 | `polychro lint --ruleset openapi-ruleset.yml step-6-openapi.yml` | 2 | `operation-id-unique` / ERROR / message LIKELY the ruleset's "Every `operationId` must be unique across the whole contract." (because the rule defines a `message`, which `RuleExecutor` prefers over the function's own message). The custom polyglot function fires because `get-ship` is reused on `/ships` GET and `/ships/{imo_number}` GET. See point 3 for the message-override caveat. | UNVERIFIED |
| 7 | `polychro lint --format agent step-7-openapi.yml` | 0 | clean — AgentFormatter JSON: `{"diagnostics": [], "summary": {"errors": 0, "warnings": 0, "info": 0}, "tokens": N}` (well-formedness only; the contract is clean). The Ikanos bridge note: `polychro:governance` is capability-centric and is run on an Ikanos **capability**, NOT on this OpenAPI file. | UNVERIFIED |

## Engine facts this table relies on (each verified by reading source)

- **Exit codes** — `polychro-cli/src/main/java/io/polychro/cli/LintCommand.java`,
  `computeExitCode`: empty → 0; any ERROR → 2; any WARN (no error) → 1; only INFO/HINT → 0.
- **Well-formedness codes** — `polychro-wellformedness/.../WellformednessValidator.java`:
  duplicate key → `duplicate-key` (ERROR); tab indentation → `tab-indentation` (ERROR);
  UTF-8 BOM → `utf8-bom` (WARN). Well-formedness has no required config so it always runs.
- **Ruleset schema** — mirrored from `polychro-rulesets/src/main/resources/rulesets/governance.yml`:
  `aliases:` + `rules:` map; each rule `{ message, description, severity, recommended, given, then }`;
  `then` = `{ field?, function, functionOptions? }`. Diagnostic `code` = rule name, `message` =
  rule's `message` (or the function's own message when `message` is null), `severity` mapped via
  `RuleExecutor.mapSeverity` (`error`/`warn`/`info`/`hint`).
- **Built-in functions** — `truthy`, `defined`, `pattern` (`match`/`notMatch`), `length`,
  `enumeration`, `falsy`, `casing`, `alphabetical` (`polychro-ruleset/src/main/java/io/polychro/ruleset/*Function.java`).
- **JSONPath** — Jayway (`com.jayway.jsonpath`), NOT Spectral. The Spectral key selector `$.paths~`
  is **unsupported** and an unparseable expression silently returns no matches
  (`JsonPathEvaluator.evaluate` swallows the exception) — so the ruleset uses value selectors only.
- **Polyglot wiring** — `functionsDir:` + `functions:` at the ruleset top level
  (`polychro-ruleset-polyglot/.../PolyglotRulesetEndToEndTest.java`). A function file does
  `export default function name(targetVal) { return [{ message, path: [...] }]; }`
  (mirrors `unique-namespaces.js`). The diagnostic `code` is the rule name, `severity` from the rule.
- **Schema-model trigger** — `polychro-core/.../Linter.java`: with an empty `validators:` list and a
  `json-schema` (or `json-structure`) config block present, the implicit schema-model validator runs.
  `--config .polychro.yml` carries that block; `schemaPath` resolves via `Path.of(...)` (relative to CWD).
- **JSON-Schema diagnostics** — `polychro-json-schema/.../JsonSchemaValidator.java`: `code` = the
  networknt `ValidationMessage.getType()` (e.g. `type`, `required`, `pattern`); always `Severity.ERROR`.
- **AgentFormatter shape** — `polychro-core/.../AgentFormatter.java`: top-level
  `{ diagnostics: [ { severity, path?, rule?, message, suggestion?, range? } ], summary: { errors, warnings, info }, tokens }`.
  Severity strings: `error`/`warning`/`info`/`hint`.

## Points the main agent MUST verify (highest risk first)

1. **Step 4 schema `code` and `path`.** The exact diagnostic `code` is the networknt
   `getType()` value (I claim `type`), and the `path` format (`$.info.version` vs `info.version`
   vs `$.info.version`) is from `JsonSchemaValidator`'s path construction — I did not run it.
   Also confirm Jackson YAML actually parses `1.0` as a non-string (float) so the `type: string`
   constraint fails. If `1.0` is read as a string, Step 4 will NOT fail — adjust the violation.
2. **Step 6 polyglot path resolution.** The function returns a `path` containing a string segment
   with slashes and braces (`/ships/{imo_number}`). Confirm `PolyglotRuleFunction.extractPath` +
   `RuleExecutor.combinePath` handle such segments without throwing, and that the rule fires once
   (not zero, not duplicated). The diagnostic still appears even if the SourceRange ends up null —
   verify the message text matches what the function emits.
3. **Step 6 message text.** Running the JS logic against the realigned `step-6-openapi.yml` (where
   `get-ship` is reused on `/ships` GET and `/ships/{imo_number}` GET), the function's own message
   is `operationId 'get-ship' is already used at paths//ships/get/operationId. Every operationId
   must be unique across the whole contract.` and the per-violation `path` is
   `["paths","/ships/{imo_number}","get","operationId"]`. This is DERIVED FROM READING THE FUNCTION,
   not re-run in Node after the realignment — treat the exact string as UNVERIFIED. What also remains
   UNVERIFIED is whether the engine's `RuleExecutor.combinePath` (with `given: $`) and
   `PolyglotRuleFunction.extractPath` reshape that path into the diagnostic's `path` field, and
   whether the rule's `message` (set in the ruleset) OVERRIDES the function's message — per
   `RuleExecutor.execute`, `rule.message()` wins when non-null, so the diagnostic message is likely
   the ruleset's "Every `operationId` must be unique across the whole contract." NOT the function's
   longer string. Verify which message actually surfaces.
4. **Step 3 / Step 5 single-fire assumption.** I designed each file so that exactly one rule fires.
   Verify no *other* rule in `openapi-ruleset.yml` also fires on these files (e.g. that
   `operation-responses-present` and `operation-id-present` truly stay green on Step 3, and that
   `path-summary-present` and `operation-responses-present` stay green on Step 5 — only the `/crew`
   GET drops its `operationId`, every path item keeps its `summary` and every operation keeps its
   `responses`). If extra diagnostics appear, the exit code may still match but the "expected
   diagnostic" cell must list them all.
5. **Step 1/2 skip behaviour.** Confirm that bare `polychro lint` (no `--ruleset`/`--schema`)
   really runs ONLY well-formedness and silently skips the ruleset/json-schema factories
   (`createValidator` swallows missing-config for auto-discovered factories). If any other
   auto-discovered validator (markdown/html/json-structure) emits something on a `.yml`, Step 1's
   "exit 0 / No issues found." claim breaks.
