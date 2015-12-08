import { getJSON } from '../helpers/request.js';

export function getTimeMachineData (componentKey, metrics) {
  const url = baseUrl + '/api/timemachine/index';
  const data = { resource: componentKey, metrics };
  return getJSON(url, data);
}
