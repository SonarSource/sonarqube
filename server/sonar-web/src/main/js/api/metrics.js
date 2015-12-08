import { getJSON } from '../helpers/request.js';

export function getMetrics () {
  const url = baseUrl + '/api/metrics/search';
  const data = { ps: 9999 };
  return getJSON(url, data).then(r => r.metrics);
}
