import _ from 'underscore';

import { getJSON } from '../helpers/request.js';


export function getFacets (query, facets) {
  const url = baseUrl + '/api/issues/search';
  const data = _.extend({}, query, { facets: facets.join(), ps: 1, additionalFields: '_all' });
  return getJSON(url, data).then(r => {
    return { facets: r.facets, response: r };
  });
}


export function getFacet (query, facet) {
  return getFacets(query, [facet]).then(r => {
    return { facet: r.facets[0].values, response: r.response };
  });
}


export function getSeverities (query) {
  return getFacet(query, 'severities').then(r => r.facet);
}


export function getTags (query) {
  return getFacet(query, 'tags').then(r => r.facet);
}


export function extractAssignees (facet, response) {
  return facet.map(item => {
    const user = _.findWhere(response.users, { login: item.val });
    return _.extend(item, { user });
  });
}


export function getAssignees (query) {
  return getFacet(query, 'assignees').then(r => extractAssignees(r.facet, r.response));
}


export function getIssuesCount (query) {
  const url = baseUrl + '/api/issues/search';
  const data = _.extend({}, query, { ps: 1, facetMode: 'debt' });
  return getJSON(url, data).then(r => {
    return { issues: r.total, debt: r.debtTotal };
  });
}
