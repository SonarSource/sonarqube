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
// @flow
import { searchIssues } from '../../../api/issues';
import { parseIssueFromResponse } from '../../../helpers/issues';

/*::
export type Query = { [string]: string | void };
*/

/*::
export type Issues = Array<*>; */

// maximum possible value
const PAGE_SIZE = 500;

function buildQuery(component /*: string */, branch /*: string | void */) /*: Query */ {
  return {
    additionalFields: '_all',
    resolved: 'false',
    componentKeys: component,
    branch,
    s: 'FILE_LINE'
  };
}

export function loadPage(
  query /*: Query */,
  page /*: number */,
  pageSize /*: number */ = PAGE_SIZE
) /*: Promise<Issues> */ {
  return searchIssues({
    ...query,
    p: page,
    ps: pageSize
  }).then(r =>
    r.issues.map(issue => parseIssueFromResponse(issue, r.components, r.users, r.rules))
  );
}

export function loadPageAndNext(
  query /*: Query */,
  toLine /*: number */,
  page /*: number */,
  pageSize /*: number */ = PAGE_SIZE
) /*: Promise<Issues> */ {
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
  component /*: string */,
  fromLine /*: number */,
  toLine /*: number */,
  branch /*: string | void */
) /*: Promise<Issues> */ {
  const query = buildQuery(component, branch);
  return new Promise(resolve => {
    loadPageAndNext(query, toLine, 1).then(issues => {
      resolve(issues);
    });
  });
}
