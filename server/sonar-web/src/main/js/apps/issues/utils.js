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
import { searchMembers } from '../../api/organizations';
import { searchUsers } from '../../api/users';
import { formatMeasure } from '../../helpers/measures';
import {
  queriesEqual,
  cleanQuery,
  parseAsBoolean,
  parseAsFacetMode,
  parseAsArray,
  parseAsString,
  serializeString,
  serializeStringArray
} from '../../helpers/query';
/*:: import type { RawQuery } from '../../helpers/query'; */

/*::
export type Query = {|
  assigned: boolean,
  assignees: Array<string>,
  authors: Array<string>,
  createdAfter: string,
  createdAt: string,
  createdBefore: string,
  createdInLast: string,
  directories: Array<string>,
  facetMode: string,
  files: Array<string>,
  issues: Array<string>,
  languages: Array<string>,
  modules: Array<string>,
  projects: Array<string>,
  resolved: boolean,
  resolutions: Array<string>,
  rules: Array<string>,
  sort: string,
  severities: Array<string>,
  sinceLeakPeriod: boolean,
  statuses: Array<string>,
  tags: Array<string>,
  types: Array<string>
|};
*/

/*::
export type Paging = {
  pageIndex: number,
  pageSize: number,
  total: number
};
*/

// allow sorting by CREATION_DATE only
const parseAsSort = (sort /*: string */) => (sort === 'CREATION_DATE' ? 'CREATION_DATE' : '');

export function parseQuery(query /*: RawQuery */) /*: Query */ {
  return {
    assigned: parseAsBoolean(query.assigned),
    assignees: parseAsArray(query.assignees, parseAsString),
    authors: parseAsArray(query.authors, parseAsString),
    createdAfter: parseAsString(query.createdAfter),
    createdAt: parseAsString(query.createdAt),
    createdBefore: parseAsString(query.createdBefore),
    createdInLast: parseAsString(query.createdInLast),
    directories: parseAsArray(query.directories, parseAsString),
    facetMode: parseAsFacetMode(query.facetMode),
    files: parseAsArray(query.fileUuids, parseAsString),
    issues: parseAsArray(query.issues, parseAsString),
    languages: parseAsArray(query.languages, parseAsString),
    modules: parseAsArray(query.moduleUuids, parseAsString),
    projects: parseAsArray(query.projectUuids, parseAsString),
    resolved: parseAsBoolean(query.resolved),
    resolutions: parseAsArray(query.resolutions, parseAsString),
    rules: parseAsArray(query.rules, parseAsString),
    sort: parseAsSort(query.s),
    severities: parseAsArray(query.severities, parseAsString),
    sinceLeakPeriod: parseAsBoolean(query.sinceLeakPeriod, false),
    statuses: parseAsArray(query.statuses, parseAsString),
    tags: parseAsArray(query.tags, parseAsString),
    types: parseAsArray(query.types, parseAsString)
  };
}

export function getOpen(query /*: RawQuery */) /*: string */ {
  return query.open;
}

export const areMyIssuesSelected = (query /*: RawQuery */) => query.myIssues === 'true';

export function serializeQuery(query /*: Query */) /*: RawQuery */ {
  const filter = {
    assigned: query.assigned ? undefined : 'false',
    assignees: serializeStringArray(query.assignees),
    authors: serializeStringArray(query.authors),
    createdAfter: serializeString(query.createdAfter),
    createdAt: serializeString(query.createdAt),
    createdBefore: serializeString(query.createdBefore),
    createdInLast: serializeString(query.createdInLast),
    directories: serializeStringArray(query.directories),
    facetMode: query.facetMode === 'effort' ? serializeString(query.facetMode) : undefined,
    fileUuids: serializeStringArray(query.files),
    issues: serializeStringArray(query.issues),
    languages: serializeStringArray(query.languages),
    moduleUuids: serializeStringArray(query.modules),
    projectUuids: serializeStringArray(query.projects),
    resolved: query.resolved ? undefined : 'false',
    resolutions: serializeStringArray(query.resolutions),
    s: serializeString(query.sort),
    severities: serializeStringArray(query.severities),
    sinceLeakPeriod: query.sinceLeakPeriod ? 'true' : undefined,
    statuses: serializeStringArray(query.statuses),
    rules: serializeStringArray(query.rules),
    tags: serializeStringArray(query.tags),
    types: serializeStringArray(query.types)
  };
  return cleanQuery(filter);
}

export const areQueriesEqual = (a /*: RawQuery */, b /*: RawQuery */) =>
  queriesEqual(parseQuery(a), parseQuery(b));

/*::
type RawFacet = {
  property: string,
  values: Array<{ val: string, count: number }>
};
*/

/*::
export type Facet = { [string]: number };
*/

export function mapFacet(facet /*: string */) /*: string */ {
  const propertyMapping = {
    files: 'fileUuids',
    modules: 'moduleUuids',
    projects: 'projectUuids'
  };
  return propertyMapping[facet] || facet;
}

export function parseFacets(facets /*: Array<RawFacet> */) /*: { [string]: Facet } */ {
  // for readability purpose
  const propertyMapping = {
    fileUuids: 'files',
    moduleUuids: 'modules',
    projectUuids: 'projects'
  };

  const result = {};
  facets.forEach(facet => {
    const values = {};
    facet.values.forEach(value => {
      values[value.val] = value.count;
    });
    const finalProperty = propertyMapping[facet.property] || facet.property;
    result[finalProperty] = values;
  });
  return result;
}

export function formatFacetStat(stat /*: ?number */, mode /*: string */) /*: string | void */ {
  if (stat != null) {
    return formatMeasure(stat, mode === 'effort' ? 'SHORT_WORK_DUR' : 'SHORT_INT');
  }
}

/*::
export type ReferencedComponent = {
  key: string,
  name: string,
  organization: string,
  path: string
};
*/

/*::
export type ReferencedUser = {
  avatar: string,
  name: string
};
*/

/*::
export type ReferencedLanguage = {
  name: string
};
*/

/*::
export type Component = {
  key: string,
  name: string,
  organization: string,
  qualifier: string
};
*/

/*::
export type CurrentUser =
  | { isLoggedIn: false }
  | { isLoggedIn: true, avatar:string, email?: string, login: string, name: string };
*/

export const searchAssignees = (query /*: string */, organization /*: ?string */) => {
  return organization
    ? searchMembers({ organization, ps: 50, q: query }).then(response =>
        response.users.map(user => ({
          avatar: user.avatar,
          label: user.name,
          value: user.login
        }))
      )
    : searchUsers({ q: query }).then(response =>
        response.users.map(user => ({
          // TODO this WS returns no avatar
          avatar: user.avatar,
          email: user.email,
          label: user.name,
          value: user.login
        }))
      );
};

const LOCALSTORAGE_KEY = 'sonarqube.issues.default';
const LOCALSTORAGE_MY = 'my';
const LOCALSTORAGE_ALL = 'all';

export const isMySet = () => {
  const setting = window.localStorage.getItem(LOCALSTORAGE_KEY);
  return setting === LOCALSTORAGE_MY;
};

const save = (value /*: string */) => {
  try {
    window.localStorage.setItem(LOCALSTORAGE_KEY, value);
  } catch (e) {
    // usually that means the storage is full
    // just do nothing in this case
  }
};

export const saveMyIssues = (myIssues /*: boolean */) =>
  save(myIssues ? LOCALSTORAGE_MY : LOCALSTORAGE_ALL);
