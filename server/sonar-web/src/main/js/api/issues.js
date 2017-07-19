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
import { getJSON, post, postJSON } from '../helpers/request';

export type IssueResponse = {
  components?: Array<*>,
  issue: {},
  rules?: Array<*>,
  users?: Array<*>
};

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
    const user = response.users ? response.users.find(user => user.login === item.val) : null;
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

export const searchIssueTags = (
  data: { organization?: string, ps?: number, q?: string } = { ps: 500 }
): Promise<Array<string>> => getJSON('/api/issues/tags', data).then(r => r.tags);

export function getIssueChangelog(issue: string): Promise<*> {
  const url = '/api/issues/changelog';
  return getJSON(url, { issue }).then(r => r.changelog);
}

export function getIssueFilters() {
  const url = '/api/issue_filters/search';
  return getJSON(url).then(r => r.issueFilters);
}

export function addIssueComment(data: { issue: string, text: string }): Promise<IssueResponse> {
  const url = '/api/issues/add_comment';
  return postJSON(url, data);
}

export function deleteIssueComment(data: { comment: string }): Promise<IssueResponse> {
  const url = '/api/issues/delete_comment';
  return postJSON(url, data);
}

export function editIssueComment(data: { comment: string, text: string }): Promise<IssueResponse> {
  const url = '/api/issues/edit_comment';
  return postJSON(url, data);
}

export function setIssueAssignee(data: {
  issue: string,
  assignee?: string
}): Promise<IssueResponse> {
  const url = '/api/issues/assign';
  return postJSON(url, data);
}

export function setIssueSeverity(data: { issue: string, severity: string }): Promise<*> {
  const url = '/api/issues/set_severity';
  return postJSON(url, data);
}

export function setIssueTags(data: { issue: string, tags: string }): Promise<IssueResponse> {
  const url = '/api/issues/set_tags';
  return postJSON(url, data);
}

export function setIssueTransition(data: {
  issue: string,
  transition: string
}): Promise<IssueResponse> {
  const url = '/api/issues/do_transition';
  return postJSON(url, data);
}

export function setIssueType(data: { issue: string, type: string }): Promise<IssueResponse> {
  const url = '/api/issues/set_type';
  return postJSON(url, data);
}

export const bulkChangeIssues = (issueKeys: Array<string>, query: {}) =>
  post('/api/issues/bulk_change', {
    issues: issueKeys.join(),
    ...query
  });
