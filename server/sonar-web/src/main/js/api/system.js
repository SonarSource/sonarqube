import { getJSON, post } from '../helpers/request';

export function setLogLevel (level) {
  const url = window.baseUrl + '/api/system/change_log_level';
  const data = { level };
  return post(url, data);
}

export function getSystemInfo () {
  const url = window.baseUrl + '/api/system/info';
  return getJSON(url);
}
