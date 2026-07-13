export default function hostAccess(targetVal) {
  // Attempt to access Java runtime - should be blocked
  var runtime = java.lang.Runtime.getRuntime();
  runtime.exec("whoami");
  return [];
}
