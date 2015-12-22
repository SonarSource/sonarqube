import { getJSON, postJSON, post } from '../helpers/request.js';


export function getComponents (data) {
  const url = baseUrl + '/api/components/search';
  return getJSON(url, data);
}

export function getProvisioned (data) {
  const url = baseUrl + '/api/projects/provisioned';
  return getJSON(url, data);
}

export function getGhosts (data) {
  const url = baseUrl + '/api/projects/ghosts';
  return getJSON(url, data);
}

export function deleteComponents (data) {
  const url = baseUrl + '/api/projects/bulk_delete';
  return post(url, data);
}

export function createProject (data) {
  const url = baseUrl + '/api/projects/create';
  return postJSON(url, data);
}

export function getChildren (componentKey, metrics = []) {
  const url = baseUrl + '/api/resources/index';
  const data = { resource: componentKey, metrics: metrics.join(','), depth: 1 };
  return getJSON(url, data);
}

export function getFiles (componentKey, metrics = []) {
  // due to the limitation of the WS we can not ask qualifiers=FIL,
  // in this case the WS does not return measures
  // so the filtering by a qualifier is done manually

  const url = baseUrl + '/api/resources/index';
  const data = { resource: componentKey, metrics: metrics.join(','), depth: -1 };
  return getJSON(url, data).then(r => {
    return r.filter(component => component.qualifier === 'FIL');
  });
}

export function getComponent (componentKey, metrics = []) {
  const url = baseUrl + '/api/resources/index';
  const data = { resource: componentKey, metrics: metrics.join(',') };
  return getJSON(url, data).then(r => r[0]);
}

export function getTree(baseComponentKey, options = {}) {
  const url = baseUrl + '/api/components/tree';
  const data = Object.assign({}, options, { baseComponentKey });
  return getJSON(url, data);
}
