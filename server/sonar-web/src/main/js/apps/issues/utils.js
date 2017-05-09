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
import { isNil, omitBy } from 'lodash';
import { searchMembers } from '../../api/organizations';
import { searchUsers } from '../../api/users';

export type RawQuery = { [string]: string };

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

export type Paging = {
  pageIndex: number,
  pageSize: number,
  total: number
};

const parseAsBoolean = (value: ?string, defaultValue: boolean = true): boolean =>
  (value === 'false' ? false : value === 'true' ? true : defaultValue);

const parseAsString = (value: ?string): string => value || '';

const parseAsStringArray = (value: ?string): Array<string> => (value ? value.split(',') : []);

const parseAsFacetMode = (facetMode: string) =>
  (facetMode === 'debt' || facetMode === 'effort' ? 'effort' : 'count');

// allow sorting by CREATION_DATE only
const parseAsSort = (sort: string): string => (sort === 'CREATION_DATE' ? 'CREATION_DATE' : '');

export const parseQuery = (query: RawQuery): Query => ({
  assigned: parseAsBoolean(query.assigned),
  assignees: parseAsStringArray(query.assignees),
  authors: parseAsStringArray(query.authors),
  createdAfter: parseAsString(query.createdAfter),
  createdAt: parseAsString(query.createdAt),
  createdBefore: parseAsString(query.createdBefore),
  createdInLast: parseAsString(query.createdInLast),
  directories: parseAsStringArray(query.directories),
  facetMode: parseAsFacetMode(query.facetMode),
  files: parseAsStringArray(query.fileUuids),
  issues: parseAsStringArray(query.issues),
  languages: parseAsStringArray(query.languages),
  modules: parseAsStringArray(query.moduleUuids),
  projects: parseAsStringArray(query.projectUuids),
  resolved: parseAsBoolean(query.resolved),
  resolutions: parseAsStringArray(query.resolutions),
  rules: parseAsStringArray(query.rules),
  sort: parseAsSort(query.s),
  severities: parseAsStringArray(query.severities),
  sinceLeakPeriod: parseAsBoolean(query.sinceLeakPeriod, false),
  statuses: parseAsStringArray(query.statuses),
  tags: parseAsStringArray(query.tags),
  types: parseAsStringArray(query.types)
});

export const getOpen = (query: RawQuery) => query.open;

export const areMyIssuesSelected = (query: RawQuery): boolean => query.myIssues === 'true';

const serializeString = (value: string): ?string => value || undefined;

const serializeValue = (value: Array<string>): ?string => (value.length ? value.join() : undefined);

export const serializeQuery = (query: Query): RawQuery => {
  const filter = {
    assigned: query.assigned ? undefined : 'false',
    assignees: serializeValue(query.assignees),
    authors: serializeValue(query.authors),
    createdAfter: serializeString(query.createdAfter),
    createdAt: serializeString(query.createdAt),
    createdBefore: serializeString(query.createdBefore),
    createdInLast: serializeString(query.createdInLast),
    directories: serializeValue(query.directories),
    facetMode: query.facetMode === 'effort' ? serializeString(query.facetMode) : undefined,
    fileUuids: serializeValue(query.files),
    issues: serializeValue(query.issues),
    languages: serializeValue(query.languages),
    moduleUuids: serializeValue(query.modules),
    projectUuids: serializeValue(query.projects),
    resolved: query.resolved ? undefined : 'false',
    resolutions: serializeValue(query.resolutions),
    s: serializeString(query.sort),
    severities: serializeValue(query.severities),
    sinceLeakPeriod: query.sinceLeakPeriod ? 'true' : undefined,
    statuses: serializeValue(query.statuses),
    rules: serializeValue(query.rules),
    tags: serializeValue(query.tags),
    types: serializeValue(query.types)
  };
  return omitBy(filter, isNil);
};

const areArraysEqual = (a: Array<string>, b: Array<string>) => {
  if (a.length !== b.length) {
    return false;
  }
  for (let i = 0; i < a.length; i++) {
    if (a[i] !== b[i]) {
      return false;
    }
  }
  return true;
};

export const areQueriesEqual = (a: RawQuery, b: RawQuery) => {
  const parsedA: Query = parseQuery(a);
  const parsedB: Query = parseQuery(b);

  const keysA = Object.keys(parsedA);
  const keysB = Object.keys(parsedB);

  if (keysA.length !== keysB.length) {
    return false;
  }

  return keysA.every(
    key =>
      (Array.isArray(parsedA[key]) && Array.isArray(parsedB[key])
        ? areArraysEqual(parsedA[key], parsedB[key])
        : parsedA[key] === parsedB[key])
  );
};

type RawFacet = {
  property: string,
  values: Array<{ val: string, count: number }>
};

export type Facet = { [string]: number };

export const mapFacet = (facet: string): string => {
  const propertyMapping = {
    files: 'fileUuids',
    modules: 'moduleUuids',
    projects: 'projectUuids'
  };
  return propertyMapping[facet] || facet;
};

export const parseFacets = (facets: Array<RawFacet>): { [string]: Facet } => {
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
};

export type ReferencedComponent = {
  key: string,
  name: string,
  organization: string,
  path: string
};

export type ReferencedUser = {
  avatar: string,
  name: string
};

export type ReferencedLanguage = {
  name: string
};

export type Component = {
  key: string,
  name: string,
  organization: string,
  qualifier: string
};

export type CurrentUser =
  | { isLoggedIn: false }
  | { isLoggedIn: true, email?: string, login: string, name: string };

export const searchAssignees = (query: string, component?: Component) => {
  return component
    ? searchMembers({ organization: component.organization, ps: 50, q: query }).then(response =>
        response.users.map(user => ({
          avatar: user.avatar,
          label: user.name,
          value: user.login
        }))
      )
    : searchUsers(query, 50).then(response =>
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

export const isMySet = (): boolean => {
  const setting = window.localStorage.getItem(LOCALSTORAGE_KEY);
  return setting === LOCALSTORAGE_MY;
};

const save = (value: string) => {
  try {
    window.localStorage.setItem(LOCALSTORAGE_KEY, value);
  } catch (e) {
    // usually that means the storage is full
    // just do nothing in this case
  }
};

export const saveMyIssues = (myIssues: boolean) =>
  save(myIssues ? LOCALSTORAGE_MY : LOCALSTORAGE_ALL);
