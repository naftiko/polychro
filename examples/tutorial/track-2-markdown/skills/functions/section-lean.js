// section-lean.js a custom Polychro polyglot rule function.
//
// Contract (mirrors track-1's functions/operation-id-unique.js):
//   - export a single default function taking `targetVal` (the JSON value at
//     the rule's `given:` path here the whole document, because given is "$").
//   - return an array of result objects, one per violation, each shaped
//     { message: string, path: [segments] }. An empty array means "no violation".
//
// Counts words per section (the text between one heading and the next) and
// flags any section over `maxWords` Polychro's oriented rulesets can only
// express quantifiable leanness, not judge whether prose is actually clear.
export default function sectionLean(targetVal) {
  if (!targetVal || typeof targetVal !== "object") return [];
  const doc = targetVal.document;
  if (!doc || !Array.isArray(doc.blocks)) return [];

  const maxWords = 30;
  const results = [];
  let heading = null;
  let headingIndex = -1;
  let words = 0;

  function countWords(text) {
    return (text.match(/\S+/g) || []).length;
  }

  function flush() {
    if (heading !== null && words > maxWords) {
      results.push({
        message: `Section "${heading}" is ${words} words. Keep each section under ${maxWords} words.`,
        path: ["document", "blocks", headingIndex],
      });
    }
  }

  doc.blocks.forEach((block, i) => {
    if (block.type === "heading") {
      flush();
      heading = block.text;
      headingIndex = i;
      words = 0;
    } else if (block.type === "paragraph") {
      words += countWords(block.text || "");
    } else if (block.type === "list") {
      (block.items || []).forEach((item) => {
        words += countWords(item.text || "");
      });
    }
  });
  flush();

  return results;
}
