import _ from 'underscore';
import { getJSON } from '../helpers/request.js';

function getFacet (query, facet) {
  let url = baseUrl + '/api/issues/search';
  let data = _.extend({}, query, { facets: facet, ps: 1, additionalFields: '_all' });
  return getJSON(url, data).then(r => {
    return { facet: r.facets[0].values, response: r };
  });
}

export function getSeverities (query) {
  return getFacet(query, 'severities').then(r => r.facet);
}

export function getTags (query) {
  return getFacet(query, 'tags').then(r => r.facet);
}

export function getAssignees (query) {
  return getFacet(query, 'assignees').then(r => {
    return r.facet.map(item => {
      let user = _.findWhere(r.response.users, { login: item.val });
      return _.extend(item, { user });
    });
  });
}
