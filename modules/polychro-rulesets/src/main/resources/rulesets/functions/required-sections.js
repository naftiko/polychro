// required-sections — generic, parameterized replacement for N hardcoded
// "section X must exist" rules. Takes the projected Markdown
// $.document.blocks array and reports one violation per heading text missing
// from functionOptions.sections. Reused by both the AGENTS.md and SKILL.md
// overrides in agents.yml with different `sections` options.
export default function requiredSections(blocks, options) {
  if (!Array.isArray(blocks)) {
    return [];
  }
  var required = (options && options.sections) || [];
  var headings = blocks
    .filter(function (block) {
      return block && block.type === "heading";
    })
    .map(function (block) {
      return block.text;
    });

  var results = [];
  required.forEach(function (section) {
    if (headings.indexOf(section) === -1) {
      results.push({ message: "Missing required section: ## " + section });
    }
  });
  return results;
}
