export function getProjectUrl (project) {
  if (typeof project !== 'string') {
    throw new TypeError('Project ID or KEY should be passed');
  }
  return `${window.baseUrl}/dashboard?id=${encodeURIComponent(project)}`;
}

export function componentIssuesUrl (componentKey, query) {
  let serializedQuery = Object.keys(query).map(criterion => {
    return `${encodeURIComponent(criterion)}=${encodeURIComponent(query[criterion])}`;
  }).join('|');
  return window.baseUrl + '/component_issues?id=' + encodeURIComponent(componentKey) + '#' + serializedQuery;
}
