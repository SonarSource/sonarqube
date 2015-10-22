import { getJSON, post } from '../helpers/request';

export function setLogLevel (level) {
  let url = window.baseUrl + '/api/system/change_log_level';
  let data = { level };
  return post(url, data);
}

export function getSystemInfo () {
  let url = window.baseUrl + '/api/system/info';
  return getJSON(url);
}
