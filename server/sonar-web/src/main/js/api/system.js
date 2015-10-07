import $ from 'jquery';

export function setLogLevel (level) {
  let url = baseUrl + '/api/system/change_log_level';
  return $.post(url, { level });
}
