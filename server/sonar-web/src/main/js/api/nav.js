import { getJSON } from '../helpers/request.js';

export function getGlobalNavigation () {
  const url = baseUrl + '/api/navigation/global';
  return getJSON(url);
}

export function getComponentNavigation (componentKey) {
  const url = baseUrl + '/api/navigation/component';
  const data = { componentKey };
  return getJSON(url, data);
}

export function getSettingsNavigation () {
  const url = baseUrl + '/api/navigation/settings';
  return getJSON(url);
}
