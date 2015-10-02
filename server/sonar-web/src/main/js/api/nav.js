import { getJSON } from '../helpers/request.js';

export function getGlobalNavigation () {
  let url = baseUrl + '/api/navigation/global';
  return getJSON(url);
}

export function getComponentNavigation (componentKey) {
  let url = baseUrl + '/api/navigation/component';
  let data = { componentKey };
  return getJSON(url, data);
}

export function getSettingsNavigation () {
  let url = baseUrl + '/api/navigation/settings';
  return getJSON(url);
}
