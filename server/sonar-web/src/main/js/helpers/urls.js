/**
 * Generate URL for a component's home page
 * @param {string} componentKey
 * @returns {string}
 */
export function getComponentUrl (componentKey) {
  return window.baseUrl + '/dashboard?id=' + encodeURIComponent(componentKey);
}


/**
 * Generate URL for a component's issues page
 * @param {string} componentKey
 * @param {object} query
 * @returns {string}
 */
export function getComponentIssuesUrl (componentKey, query) {
  let serializedQuery = Object.keys(query).map(criterion => {
    return `${encodeURIComponent(criterion)}=${encodeURIComponent(query[criterion])}`;
  }).join('|');
  return window.baseUrl + '/component_issues?id=' + encodeURIComponent(componentKey) + '#' + serializedQuery;
}


/**
 * Generate URL for a component's drilldown page
 * @param {string} componentKey
 * @param {string} metric
 * @param {string|number} [period]
 * @returns {string}
 */
export function getComponentDrilldownUrl (componentKey, metric, period) {
  let url = window.baseUrl + '/drilldown/measures?id=' + encodeURIComponent(componentKey) +
      '&metric=' + encodeURIComponent(metric);
  if (period) {
    url += '&period=' + period;
  }
  return url;
}
