/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { getJSON, post, postJSON, RequestData } from '../helpers/request';
import { RawIssue } from '../helpers/issues';

export interface IssueResponse {
  components?: Array<{}>;
  issue: {};
  rules?: Array<{}>;
  users?: Array<{}>;
}

interface IssuesResponse {
  components?: { key: string; name: string; uuid: string }[];
  debtTotal?: number;
  facets: Array<{}>;
  issues: RawIssue[];
  paging: {
    pageIndex: number;
    pageSize: number;
    total: number;
  };
  rules?: Array<{}>;
  users?: { login: string }[];
}

export function searchIssues(query: RequestData): Promise<IssuesResponse> {
  return getJSON('/api/issues/search', query);
}

export function getFacets(query: RequestData, facets: string[]): Promise<any> {
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

export function getFacet(
  query: RequestData,
  facet: string
): Promise<{ facet: { count: number; val: string }[]; response: IssuesResponse }> {
  return getFacets(query, [facet]).then(r => {
    return { facet: r.facets[0].values, response: r.response };
  });
}

export function getSeverities(query: RequestData): Promise<any> {
  return getFacet(query, 'severities').then(r => r.facet);
}

export function getTags(query: RequestData): Promise<any> {
  return getFacet(query, 'tags').then(r => r.facet);
}

export function extractAssignees(facet: Array<{ val: string }>, response: IssuesResponse): any {
  return facet.map(item => {
    const user = response.users ? response.users.find(user => user.login === item.val) : null;
    return { ...item, user };
  });
}

export function getAssignees(query: RequestData): Promise<any> {
  return getFacet(query, 'assignees').then(r => extractAssignees(r.facet, r.response));
}

export function extractProjects(facet: { val: string }[], response: IssuesResponse) {
  return facet.map(item => {
    const project =
      response.components && response.components.find(component => component.uuid === item.val);
    return { ...item, project };
  });
}

export function getProjects(query: RequestData) {
  return getFacet(query, 'projectUuids').then(r => extractProjects(r.facet, r.response));
}

export function getIssuesCount(query: RequestData): Promise<any> {
  const data = { ...query, ps: 1, facetMode: 'effort' };
  return searchIssues(data).then(r => {
    return { issues: r.paging.total, debt: r.debtTotal };
  });
}

export function searchIssueTags(
  data: { organization?: string; ps?: number; q?: string } = { ps: 100 }
): Promise<string[]> {
  return getJSON('/api/issues/tags', data).then(r => r.tags);
}

export function getIssueChangelog(issue: string): Promise<any> {
  return getJSON('/api/issues/changelog', { issue }).then(r => r.changelog);
}

export function getIssueFilters() {
  return getJSON('/api/issue_filters/search').then(r => r.issueFilters);
}

export function addIssueComment(data: { issue: string; text: string }): Promise<IssueResponse> {
  return postJSON('/api/issues/add_comment', data);
}

export function deleteIssueComment(data: { comment: string }): Promise<IssueResponse> {
  return postJSON('/api/issues/delete_comment', data);
}

export function editIssueComment(data: { comment: string; text: string }): Promise<IssueResponse> {
  return postJSON('/api/issues/edit_comment', data);
}

export function setIssueAssignee(data: {
  issue: string;
  assignee?: string;
}): Promise<IssueResponse> {
  return postJSON('/api/issues/assign', data);
}

export function setIssueSeverity(data: { issue: string; severity: string }): Promise<any> {
  return postJSON('/api/issues/set_severity', data);
}

export function setIssueTags(data: { issue: string; tags: string }): Promise<IssueResponse> {
  return postJSON('/api/issues/set_tags', data);
}

export function setIssueTransition(data: {
  issue: string;
  transition: string;
}): Promise<IssueResponse> {
  return postJSON('/api/issues/do_transition', data);
}

export function setIssueType(data: { issue: string; type: string }): Promise<IssueResponse> {
  return postJSON('/api/issues/set_type', data);
}

export function bulkChangeIssues(issueKeys: string[], query: RequestData): Promise<void> {
  return post('/api/issues/bulk_change', { issues: issueKeys.join(), ...query });
}
