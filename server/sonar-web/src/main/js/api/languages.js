import { getJSON } from '../helpers/request.js';

export function getLanguages () {
  let url = baseUrl + '/api/languages/list';
  return getJSON(url).then(r => r.languages);
}
