// instructions-applyto-not-blank — mirrors io.polychro.markdown.InstructionsFormat's second
// check for .instructions.md / .prompt.md files: when 'applyTo' is present as a string, it
// must not be blank.
//
// Takes the projected $.document.frontmatter node, which is `null` when the document has no
// frontmatter block.
export default function instructionsApplyToNotBlank(target, options) {
  if (!target || typeof target !== "object") {
    // No frontmatter at all — nothing to check.
    return [];
  }
  var applyTo = target.applyTo;
  if (typeof applyTo === "string" && applyTo.trim().length === 0) {
    return [{ message: "'applyTo' must not be empty" }];
  }
  return [];
}
