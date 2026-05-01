export default function multiResult(targetVal) {
  return [
    { message: "First issue", path: ["info", "name"] },
    { message: "Second issue", path: ["info", "version"] }
  ];
}
