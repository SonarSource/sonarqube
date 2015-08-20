export function getProjectUrl(project) {
  if (typeof project !== 'string') {
    throw new TypeError("Project ID or KEY should be passed");
  }
  return `${window.baseUrl}/overview?id=${encodeURIComponent(project)}`;
}
