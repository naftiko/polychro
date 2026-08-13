// instructions-applyto-presence — mirrors io.polychro.markdown.InstructionsFormat's first
// check for .instructions.md / .prompt.md files: frontmatter is optional (a document with no
// frontmatter at all is acceptable and produces no violation), but when frontmatter IS present
// it should declare an 'applyTo' pattern.
//
// Takes the projected $.document.frontmatter node, which is `null` when the document has no
// frontmatter block.
export default function instructionsApplyToPresence(target, options) {
  if (!target || typeof target !== "object") {
    // No frontmatter at all — acceptable, falls back to generic handling.
    return [];
  }
  if (!("applyTo" in target)) {
    return [{ message: "Frontmatter should include 'applyTo' pattern" }];
  }
  return [];
}
