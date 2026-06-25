// operation-id-unique.js — a custom Polychro polyglot rule function.
//
// Contract (mirrors polychro-ruleset-polyglot/src/test/resources/functions/unique-namespaces.js):
//   - export a single default function taking `targetVal` (the JSON value at the
//     rule's `given:` path — here the whole document, because given is "$").
//   - return an array of result objects, one per violation, each shaped
//     { message: string, path: [segments] }. An empty array means "no violation".
//   - the engine combines each per-violation `path` with the rule's given path and
//     resolves it to a SourceRange; the diagnostic `code` is the rule name
//     (operation-id-unique) and the `severity` comes from the rule (error).
//
// This function enforces that every `operationId` is unique across the contract.

export default function operationIdUnique(targetVal) {
  if (!targetVal || typeof targetVal !== "object") {
    return [];
  }

  const paths = targetVal.paths;
  if (!paths || typeof paths !== "object") {
    return [];
  }

  const httpMethods = ["get", "put", "post", "delete", "patch", "options", "head", "trace"];
  const seen = new Map();
  const results = [];

  for (const pathKey of Object.keys(paths)) {
    const pathItem = paths[pathKey];
    if (!pathItem || typeof pathItem !== "object") {
      continue;
    }

    for (const method of httpMethods) {
      const operation = pathItem[method];
      if (!operation || typeof operation !== "object") {
        continue;
      }

      const operationId = operation.operationId;
      if (typeof operationId !== "string" || operationId.length === 0) {
        continue;
      }

      const here = ["paths", pathKey, method, "operationId"];
      const prior = seen.get(operationId);
      if (prior === undefined) {
        seen.set(operationId, here);
        continue;
      }

      results.push({
        message:
          "operationId '" +
          operationId +
          "' is already used at " +
          prior.join("/") +
          ". Every operationId must be unique across the whole contract.",
        path: here,
      });
    }
  }

  return results;
}
