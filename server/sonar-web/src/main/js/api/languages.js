import { getJSON } from '../helpers/request.js';

export function getLanguages () {
  const url = baseUrl + '/api/languages/list';
  return getJSON(url).then(r => r.languages);
}
