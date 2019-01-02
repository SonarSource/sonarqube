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
import { searchIssues } from '../../../api/issues';
import { getBranchLikeQuery } from '../../../helpers/branches';
import { parseIssueFromResponse } from '../../../helpers/issues';
import { RawQuery } from '../../../helpers/query';

// maximum possible value
const PAGE_SIZE = 500;

function buildQuery(component: string, branchLike: T.BranchLike | undefined) {
  return {
    additionalFields: '_all',
    resolved: 'false',
    componentKeys: component,
    s: 'FILE_LINE',
    ...getBranchLikeQuery(branchLike)
  };
}

export function loadPage(query: RawQuery, page: number, pageSize = PAGE_SIZE): Promise<T.Issue[]> {
  return searchIssues({
    ...query,
    p: page,
    ps: pageSize
  }).then(r =>
    r.issues.map(issue => parseIssueFromResponse(issue, r.components, r.users, r.rules))
  );
}

export function loadPageAndNext(
  query: RawQuery,
  toLine: number,
  page: number,
  pageSize = PAGE_SIZE
): Promise<T.Issue[]> {
  return loadPage(query, page).then(issues => {
    if (issues.length === 0) {
      return [];
    }

    const lastIssue = issues[issues.length - 1];

    if (
      (lastIssue.textRange != null && lastIssue.textRange.endLine > toLine) ||
      issues.length < pageSize
    ) {
      return issues;
    }

    return loadPageAndNext(query, toLine, page + 1, pageSize).then(nextIssues => {
      return [...issues, ...nextIssues];
    });
  });
}

export default function loadIssues(
  component: string,
  _fromLine: number,
  toLine: number,
  branchLike: T.BranchLike | undefined
): Promise<T.Issue[]> {
  const query = buildQuery(component, branchLike);
  return new Promise(resolve => {
    loadPageAndNext(query, toLine, 1).then(issues => {
      resolve(issues);
    });
  });
}
