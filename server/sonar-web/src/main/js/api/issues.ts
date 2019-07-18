/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { getJSON, post, postJSON, RequestData } from 'sonar-ui-common/helpers/request';
import throwGlobalError from '../app/utils/throwGlobalError';
import getCoverageStatus from '../components/SourceViewer/helpers/getCoverageStatus';
import { RawIssue } from '../helpers/issues';

export interface IssueResponse {
  components?: Array<{ key: string; name: string }>;
  issue: RawIssue;
  rules?: Array<{}>;
  users?: Array<T.UserBase>;
}

interface IssuesResponse {
  components?: { key: string; organization: string; name: string }[];
  effortTotal: number;
  facets: Array<{
    property: string;
    values: { count: number; val: string }[];
  }>;
  issues: RawIssue[];
  paging: T.Paging;
  rules?: Array<{}>;
  users?: Array<T.UserBase>;
}

type FacetName =
  | 'assigned_to_me'
  | 'assignees'
  | 'authors'
  | 'createdAt'
  | 'cwe'
  | 'directories'
  | 'files'
  | 'languages'
  | 'modules'
  | 'owaspTop10'
  | 'projects'
  | 'reporters'
  | 'resolutions'
  | 'rules'
  | 'sansTop25'
  | 'severities'
  | 'statuses'
  | 'tags'
  | 'types';

export function searchIssues(query: RequestData): Promise<IssuesResponse> {
  return getJSON('/api/issues/search', query);
}

export function getFacets(
  query: RequestData,
  facets: FacetName[]
): Promise<{
  facets: Array<{ property: string; values: T.FacetValue[] }>;
  response: IssuesResponse;
}> {
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
  facet: FacetName
): Promise<{ facet: { count: number; val: string }[]; response: IssuesResponse }> {
  return getFacets(query, [facet]).then(r => {
    return { facet: r.facets[0].values, response: r.response };
  });
}

export function searchIssueTags(data: {
  organization?: string;
  project?: string;
  ps?: number;
  q?: string;
}): Promise<string[]> {
  return getJSON('/api/issues/tags', data)
    .then(r => r.tags)
    .catch(throwGlobalError);
}

export function getIssueChangelog(issue: string): Promise<{ changelog: T.IssueChangelog[] }> {
  return getJSON('/api/issues/changelog', { issue }).catch(throwGlobalError);
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

export function searchIssueAuthors(data: {
  organization?: string;
  project?: string;
  ps?: number;
  q?: string;
}): Promise<string[]> {
  return getJSON('/api/issues/authors', data).then(r => r.authors, throwGlobalError);
}

export function getIssueFlowSnippets(issueKey: string): Promise<T.Dict<T.SnippetsByComponent>> {
  return getJSON('/api/sources/issue_snippets', { issueKey }).then(result => {
    Object.keys(result).forEach(k => {
      if (result[k].sources) {
        result[k].sources = result[k].sources.reduce(
          (lineMap: T.Dict<T.SourceLine>, line: T.SourceLine) => {
            line.coverageStatus = getCoverageStatus(line);
            lineMap[line.line] = line;
            return lineMap;
          },
          {}
        );
      }
    });
    return result;
  }, throwGlobalError);
}
