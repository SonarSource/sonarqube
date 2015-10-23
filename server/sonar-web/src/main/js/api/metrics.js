import _ from 'underscore';
import { getJSON } from '../helpers/request.js';

export function getMetrics () {
  let url = baseUrl + '/api/metrics/search';
  let data = { ps: 9999 };
  return getJSON(url, data).then(r => r.metrics);
}
