import { getJSON } from '../helpers/request.js';
import $ from 'jquery';

// TODO migrate to fetch()
export function setLogLevel (level) {
  let url = baseUrl + '/api/system/change_log_level';
  return $.post(url, { level });
}

export function getSystemInfo () {
  let url = baseUrl + '/api/system/info';
  return getJSON(url);
}
