import { getJSON, postJSON, post } from '../helpers/request.js';


export function getComponents (data) {
  let url = baseUrl + '/api/components/search';
  return getJSON(url, data);
}

export function getProvisioned (data) {
  let url = baseUrl + '/api/projects/provisioned';
  return getJSON(url, data);
}

export function getGhosts (data) {
  let url = baseUrl + '/api/projects/ghosts';
  return getJSON(url, data);
}

export function deleteComponents (data) {
  let url = baseUrl + '/api/projects/bulk_delete';
  return post(url, data);
}

export function createProject (data) {
  let url = baseUrl + '/api/projects/create';
  return postJSON(url, data);
}

export function getChildren (componentKey, metrics = []) {
  let url = baseUrl + '/api/resources/index';
  let data = { resource: componentKey, metrics: metrics.join(','), depth: 1 };
  return getJSON(url, data);
}

export function getFiles (componentKey, metrics = []) {
  // due to the limitation of the WS we can not ask qualifiers=FIL,
  // in this case the WS does not return measures
  // so the filtering by a qualifier is done manually

  let url = baseUrl + '/api/resources/index';
  let data = { resource: componentKey, metrics: metrics.join(','), depth: -1 };
  return getJSON(url, data).then(r => {
    return r.filter(component => component.qualifier === 'FIL');
  });
}
