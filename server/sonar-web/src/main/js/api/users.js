import $ from 'jquery';

export function getCurrentUser () {
  const url = baseUrl + '/api/users/current';
  return $.get(url);
}
