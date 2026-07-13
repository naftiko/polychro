export default function simpleCheck(targetVal) {
  if (!targetVal || typeof targetVal !== "object") {
    return [];
  }
  const results = [];
  if (!targetVal.name || targetVal.name.length === 0) {
    results.push({ message: "name is required" });
  }
  return results;
}
