export function csvEscape (value) {
  var escaped = value.replace(/"/g, '\\"');
  return '"' + escaped + '"';
}
