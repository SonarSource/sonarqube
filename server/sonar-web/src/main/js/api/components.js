import $ from 'jquery';

export function getComponents (data) {
  let url = baseUrl + '/api/components/search';
  return $.get(url, data);
}

export function getProvisioned (data) {
  let url = baseUrl + '/api/projects/provisioned';
  return $.get(url, data);
}

export function getGhosts (data) {
  let url = baseUrl + '/api/projects/ghosts';
  return $.get(url, data);
}

export function deleteComponents (data) {
  let url = baseUrl + '/api/projects/bulk_delete';
  return $.post(url, data);
}

export function createProject (options) {
  options.url = baseUrl + '/api/projects/create';
  options.type = 'POST';
  return $.ajax(options);
}
