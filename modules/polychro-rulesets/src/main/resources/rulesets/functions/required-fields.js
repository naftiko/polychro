// required-fields — generic, parameterized field validation. Takes a
// target object (e.g. $.document.frontmatter) and, for each field spec in
// functionOptions.fields, checks presence/required, max length, and an optional
// regex pattern. Reused for SKILL.md frontmatter's `name` and `description` per
// the Agent Skills spec (https://code.claude.com/docs/en/skills):
//   - name: required, max 64 chars, lowercase letters/numbers/hyphens only,
//     must not start or end with a hyphen.
//   - description: required, non-empty, max 1024 chars.
//
// Each field spec supports:
//   name              — the frontmatter key to check (required)
//   required          — if true, missing/null/blank-string value is a violation
//   maxLength         — maximum string length (inclusive)
//   pattern           — a regex (string) the value must match
//   patternDescription — human-readable hint appended to the invalid-format message
export default function requiredFields(target, options) {
  var fields = (options && options.fields) || [];
  var results = [];

  fields.forEach(function (spec) {
    var name = spec.name;
    if (!name) {
      return;
    }
    var value = target && typeof target === "object" ? target[name] : undefined;
    var missing = value === undefined || value === null;

    if (missing) {
      if (spec.required) {
        results.push({ message: "Missing required frontmatter field: " + name });
      }
      return;
    }

    if (typeof value !== "string") {
      // Presence is satisfied, but a required field that is present with a non-string
      // value (e.g. a number/boolean) is still invalid — length/pattern checks below
      // only make sense for strings.
      if (spec.required) {
        results.push({ message: "Frontmatter field '" + name + "' must be a string" });
      }
      return;
    }

    if (spec.required && value.trim().length === 0) {
      results.push({ message: "Frontmatter field must not be empty: " + name });
      return;
    }

    if (typeof spec.maxLength === "number" && value.length > spec.maxLength) {
      results.push({
        message: "Frontmatter field '" + name + "' must be at most " + spec.maxLength
          + " characters (got " + value.length + ")"
      });
    }

    if (spec.pattern) {
      var regex;
      try {
        regex = new RegExp(spec.pattern);
      } catch (e) {
        results.push({ message: "Frontmatter field '" + name + "' has an invalid pattern configured: " + e.message });
        return;
      }
      if (!regex.test(value)) {
        var hint = spec.patternDescription ? " (" + spec.patternDescription + ")" : "";
        results.push({ message: "Frontmatter field '" + name + "' has invalid format" + hint });
      }
    }
  });

  return results;
}
