import { getJSON } from '../helpers/request.js';

export function getEvents (componentKey, categories) {
  let url = baseUrl + '/api/events';
  let data = { resource: componentKey, categories };
  return getJSON(url, data);
}
