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
 * @param {string} [highlightedMetric]
 * @returns {string}
 */
export function getComponentDrilldownUrl (componentKey, metric, period, highlightedMetric) {
  let url = window.baseUrl + '/drilldown/measures?id=' + encodeURIComponent(componentKey) +
      '&metric=' + encodeURIComponent(metric);
  if (period) {
    url += '&period=' + period;
  }
  if (highlightedMetric) {
    url += '&highlight=' + encodeURIComponent(highlightedMetric);
  }
  return url;
}


/**
 * Generate URL for a component's dashboard
 * @param {string} componentKey
 * @param {string} dashboardKey
 * @param {string} [period]
 * @returns {string}
 */
export function getComponentDashboardUrl (componentKey, dashboardKey, period) {
  let url = window.baseUrl + '/dashboard?id=' + encodeURIComponent(componentKey) +
      '&did=' + encodeURIComponent(dashboardKey);
  if (period) {
    url += '&period=' + period;
  }
  return url;
}


/**
 * Generate URL for a fixed component's dashboard (overview)
 * @param {string} componentKey
 * @param {string} dashboardKey
 * @returns {string}
 */
export function getComponentFixedDashboardUrl (componentKey, dashboardKey) {
  return window.baseUrl + '/overview' + dashboardKey + '?id=' + encodeURIComponent(componentKey);
}


/**
 * Generate URL for a component's dashboards management page
 * @param {string} componentKey
 * @returns {string}
 */
export function getComponentDashboardManagementUrl (componentKey) {
  return window.baseUrl + '/dashboards?resource=' + encodeURIComponent(componentKey);
}
