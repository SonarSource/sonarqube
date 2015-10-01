import $ from 'jquery';

export function getGlobalNavigation () {
  let url = baseUrl + '/api/navigation/global';
  return $.get(url);
}
