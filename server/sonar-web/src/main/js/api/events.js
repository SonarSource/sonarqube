import { getJSON } from '../helpers/request.js';


/**
 * Return events for a component
 * @param {string} componentKey
 * @param {string} [categories]
 * @returns {Promise}
 */
export function getEvents (componentKey, categories) {
  const url = baseUrl + '/api/events';
  const data = { resource: componentKey };
  if (categories) {
    data.categories = categories;
  }
  return getJSON(url, data);
}
