/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
// @flow
import { getJSON, post } from '../helpers/request';

type IssuesResponse = {
  components?: Array<*>,
  debtTotal?: number,
  facets: Array<*>,
  issues: Array<*>,
  paging: {
    pageIndex: number,
    pageSize: number,
    total: number
  },
  rules?: Array<*>,
  users?: Array<*>
};

export const searchIssues = (query: {}): Promise<IssuesResponse> =>
  getJSON('/api/issues/search', query);

export function getFacets(query: {}, facets: Array<string>): Promise<*> {
  const data = {
    ...query,
    facets: facets.join(),
    ps: 1,
    additionalFields: '_all'
  };
  return searchIssues(data).then(r => {
    return { facets: r.facets, response: r };
  });
}

export function getFacet(query: {}, facet: string): Promise<*> {
  return getFacets(query, [facet]).then(r => {
    return { facet: r.facets[0].values, response: r.response };
  });
}

export function getSeverities(query: {}): Promise<*> {
  return getFacet(query, 'severities').then(r => r.facet);
}

export function getTags(query: {}): Promise<*> {
  return getFacet(query, 'tags').then(r => r.facet);
}

export function extractAssignees(facet: Array<{ val: string }>, response: IssuesResponse) {
  return facet.map(item => {
    const user = response.users ? response.users.find(user => user.login = item.val) : null;
    return { ...item, user };
  });
}

export function getAssignees(query: {}): Promise<*> {
  return getFacet(query, 'assignees').then(r => extractAssignees(r.facet, r.response));
}

export function getIssuesCount(query: {}): Promise<*> {
  const data = { ...query, ps: 1, facetMode: 'effort' };
  return searchIssues(data).then(r => {
    return { issues: r.paging.total, debt: r.debtTotal };
  });
}

export const searchIssueTags = (ps: number = 500) => getJSON('/api/issues/tags', { ps });

export function getIssueFilters() {
  const url = '/api/issue_filters/search';
  return getJSON(url).then(r => r.issueFilters);
}

export const bulkChangeIssues = (issueKeys: Array<string>, query: {}) =>
  post('/api/issues/bulk_change', {
    issues: issueKeys.join(),
    ...query
  });
