// min-length.js — custom function exercising the `functionOptions` gap tracked in
// naftiko/polychro#76 (Priority 1) / #82.
//
// Spectral invokes custom functions as (targetVal, options, context) — it forwards
// `functionOptions` declared on the rule action. Polychro's PolyglotRuleFunction.evaluateViolations
// currently calls `function.execute(jsInput)` WITHOUT the options argument (naftiko/polychro#76),
// so `options` here is always `undefined` on the Polychro side today.
//
// This function's behavior depends entirely on `options.min` being present: with no options it
// is a no-op (returns no violations), so the divergence between the two engines is directly
// observable — Spectral (which receives `min`) reports a violation, Polychro (which doesn't)
// does not. See fixtures/function-options/golden-diff.json for the documented,
// currently-expected divergence. Once #82 lands, rerun with
// -Dpolychro.conformance.updateGoldenFiles=true and the golden file should collapse to an
// empty diff — that is the signal the gap has closed.
export default function minLength(targetVal, options) {
  if (typeof targetVal !== "string") {
    return [];
  }

  const min = options && typeof options.min === "number" ? options.min : undefined;
  if (min === undefined) {
    return [];
  }

  if (targetVal.length < min) {
    return [{ message: `Value length ${targetVal.length} is less than minimum ${min}` }];
  }

  return [];
}
