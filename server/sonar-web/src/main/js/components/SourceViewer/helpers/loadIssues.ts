/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { getBranchLikeQuery } from '../../../helpers/branch-like';
import { parseIssueFromResponse } from '../../../helpers/issues';
import { BranchLike } from '../../../types/branch-like';
import { Issue, RawQuery } from '../../../types/types';

// maximum possible value
const PAGE_SIZE = 500;
// Maximum issues return 20*500 for the API.
const PAGE_MAX = 20;

function buildQuery(component: string, branchLike: BranchLike | undefined) {
  return {
    additionalFields: '_all',
    resolved: 'false',
    componentKeys: component,
    s: 'FILE_LINE',
    ...getBranchLikeQuery(branchLike),
  };
}

function loadPage(query: RawQuery, page: number, pageSize = PAGE_SIZE): Promise<Issue[]> {
  return searchIssues({
    ...query,
    p: page,
    ps: pageSize,
  }).then((r) =>
    r.issues.map((issue) => parseIssueFromResponse(issue, r.components, r.users, r.rules))
  );
}

async function loadPageAndNext(query: RawQuery, page = 1, pageSize = PAGE_SIZE): Promise<Issue[]> {
  const issues = await loadPage(query, page);

  if (issues.length === 0) {
    return [];
  }

  if (issues.length < pageSize || page >= PAGE_MAX) {
    return issues;
  }

  const nextIssues = await loadPageAndNext(query, page + 1, pageSize);
  return [...issues, ...nextIssues];
}

export default function loadIssues(
  component: string,
  branchLike: BranchLike | undefined
): Promise<Issue[]> {
  const query = buildQuery(component, branchLike);
  return loadPageAndNext(query);
}
