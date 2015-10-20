import { getJSON } from '../helpers/request.js';

export function getTimeMachineData (componentKey, metrics) {
  let url = baseUrl + '/api/timemachine/index';
  let data = { resource: componentKey, metrics };
  return getJSON(url, data);
}
