import $ from 'jquery';

export function getCurrentUser () {
  let url = baseUrl + '/api/users/current';
  return $.get(url);
}
