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
import { isArray } from 'lodash';
import { searchUsers } from '../../api/users';
import { formatMeasure } from '../../helpers/measures';
import {
  cleanQuery,
  parseAsArray,
  parseAsBoolean,
  parseAsDate,
  parseAsString,
  queriesEqual,
  serializeDateShort,
  serializeString,
  serializeStringArray,
} from '../../helpers/query';
import { get, save } from '../../helpers/storage';
import { isDefined } from '../../helpers/types';
import { Facet, RawFacet } from '../../types/issues';
import { SecurityStandard } from '../../types/security';
import { Dict, Issue, Paging, RawQuery } from '../../types/types';
import { UserBase } from '../../types/users';

const OWASP_ASVS_4_0 = 'owaspAsvs-4.0';

export interface Query {
  assigned: boolean;
  assignees: string[];
  author: string[];
  createdAfter: Date | undefined;
  createdAt: string;
  createdBefore: Date | undefined;
  createdInLast: string;
  cwe: string[];
  directories: string[];
  files: string[];
  issues: string[];
  languages: string[];
  owaspTop10: string[];
  'owaspTop10-2021': string[];
  'pciDss-3.2': string[];
  'pciDss-4.0': string[];
  [OWASP_ASVS_4_0]: string[];
  owaspAsvsLevel: string;
  projects: string[];
  resolutions: string[];
  resolved: boolean;
  rules: string[];
  sansTop25: string[];
  scopes: string[];
  severities: string[];
  inNewCodePeriod: boolean;
  sonarsourceSecurity: string[];
  sort: string;
  statuses: string[];
  tags: string[];
  types: string[];
}

export const STANDARDS = 'standards';

// allow sorting by CREATION_DATE only
const parseAsSort = (sort: string) => (sort === 'CREATION_DATE' ? 'CREATION_DATE' : '');
const ISSUES_DEFAULT = 'sonarqube.issues.default';

export function parseQuery(query: RawQuery): Query {
  return {
    assigned: parseAsBoolean(query.assigned),
    assignees: parseAsArray(query.assignees, parseAsString),
    author: isArray(query.author) ? query.author : [query.author].filter(isDefined),
    createdAfter: parseAsDate(query.createdAfter),
    createdAt: parseAsString(query.createdAt),
    createdBefore: parseAsDate(query.createdBefore),
    createdInLast: parseAsString(query.createdInLast),
    cwe: parseAsArray(query.cwe, parseAsString),
    directories: parseAsArray(query.directories, parseAsString),
    files: parseAsArray(query.files, parseAsString),
    inNewCodePeriod: parseAsBoolean(query.inNewCodePeriod, false),
    issues: parseAsArray(query.issues, parseAsString),
    languages: parseAsArray(query.languages, parseAsString),
    owaspTop10: parseAsArray(query.owaspTop10, parseAsString),
    'owaspTop10-2021': parseAsArray(query['owaspTop10-2021'], parseAsString),
    'pciDss-3.2': parseAsArray(query['pciDss-3.2'], parseAsString),
    'pciDss-4.0': parseAsArray(query['pciDss-4.0'], parseAsString),
    [OWASP_ASVS_4_0]: parseAsArray(query[OWASP_ASVS_4_0], parseAsString),
    owaspAsvsLevel: parseAsString(query['owaspAsvsLevel']),
    projects: parseAsArray(query.projects, parseAsString),
    resolutions: parseAsArray(query.resolutions, parseAsString),
    resolved: parseAsBoolean(query.resolved),
    rules: parseAsArray(query.rules, parseAsString),
    sansTop25: parseAsArray(query.sansTop25, parseAsString),
    scopes: parseAsArray(query.scopes, parseAsString),
    severities: parseAsArray(query.severities, parseAsString),
    sonarsourceSecurity: parseAsArray(query.sonarsourceSecurity, parseAsString),
    sort: parseAsSort(query.s),
    statuses: parseAsArray(query.statuses, parseAsString),
    tags: parseAsArray(query.tags, parseAsString),
    types: parseAsArray(query.types, parseAsString),
  };
}

export function getOpen(query: RawQuery): string | undefined {
  return query.open;
}

export function getOpenIssue(props: { location: { query: RawQuery } }, issues: Issue[]) {
  const open = getOpen(props.location.query);
  return open ? issues.find((issue) => issue.key === open) : undefined;
}

export const areMyIssuesSelected = (query: RawQuery) => query.myIssues === 'true';

export function serializeQuery(query: Query): RawQuery {
  const filter = {
    assigned: query.assigned ? undefined : 'false',
    assignees: serializeStringArray(query.assignees),
    author: query.author,
    createdAfter: serializeDateShort(query.createdAfter),
    createdAt: serializeString(query.createdAt),
    createdBefore: serializeDateShort(query.createdBefore),
    createdInLast: serializeString(query.createdInLast),
    cwe: serializeStringArray(query.cwe),
    directories: serializeStringArray(query.directories),
    files: serializeStringArray(query.files),
    issues: serializeStringArray(query.issues),
    languages: serializeStringArray(query.languages),
    owaspTop10: serializeStringArray(query.owaspTop10),
    'owaspTop10-2021': serializeStringArray(query['owaspTop10-2021']),
    'pciDss-3.2': serializeStringArray(query['pciDss-3.2']),
    'pciDss-4.0': serializeStringArray(query['pciDss-4.0']),
    [OWASP_ASVS_4_0]: serializeStringArray(query[OWASP_ASVS_4_0]),
    owaspAsvsLevel: serializeString(query['owaspAsvsLevel']),
    projects: serializeStringArray(query.projects),
    resolutions: serializeStringArray(query.resolutions),
    resolved: query.resolved ? undefined : 'false',
    rules: serializeStringArray(query.rules),
    s: serializeString(query.sort),
    sansTop25: serializeStringArray(query.sansTop25),
    scopes: serializeStringArray(query.scopes),
    severities: serializeStringArray(query.severities),
    inNewCodePeriod: query.inNewCodePeriod ? 'true' : undefined,
    sonarsourceSecurity: serializeStringArray(query.sonarsourceSecurity),
    statuses: serializeStringArray(query.statuses),
    tags: serializeStringArray(query.tags),
    types: serializeStringArray(query.types),
  };

  return cleanQuery(filter);
}

export const areQueriesEqual = (a: RawQuery, b: RawQuery) =>
  queriesEqual(parseQuery(a), parseQuery(b));

export function parseFacets(facets: RawFacet[]): Dict<Facet> {
  if (!facets) {
    return {};
  }

  const result: Dict<Facet> = {};
  facets.forEach((facet) => {
    const values: Facet = {};
    facet.values.forEach((value) => {
      values[value.val] = value.count;
    });
    result[facet.property] = values;
  });
  return result;
}

export function formatFacetStat(stat: number | undefined) {
  return stat && formatMeasure(stat, 'SHORT_INT');
}

export const searchAssignees = (
  query: string,
  page = 1
): Promise<{ paging: Paging; results: UserBase[] }> => {
  return searchUsers({ p: page, q: query }).then(({ paging, users }) => ({
    paging,
    results: users,
  }));
};

const LOCALSTORAGE_MY = 'my';
const LOCALSTORAGE_ALL = 'all';

export const isMySet = () => {
  return get(ISSUES_DEFAULT) === LOCALSTORAGE_MY;
};

export const saveMyIssues = (myIssues: boolean) =>
  save(ISSUES_DEFAULT, myIssues ? LOCALSTORAGE_MY : LOCALSTORAGE_ALL);

export function getLocations(
  {
    flows,
    secondaryLocations,
    flowsWithType,
  }: Pick<Issue, 'flows' | 'secondaryLocations' | 'flowsWithType'>,
  selectedFlowIndex: number | undefined
) {
  if (secondaryLocations.length > 0) {
    return secondaryLocations;
  } else if (selectedFlowIndex !== undefined) {
    return flows[selectedFlowIndex] || flowsWithType[selectedFlowIndex]?.locations || [];
  }
  return [];
}

export function getSelectedLocation(
  issue: Pick<Issue, 'flows' | 'secondaryLocations' | 'flowsWithType'>,
  selectedFlowIndex: number | undefined,
  selectedLocationIndex: number | undefined
) {
  const locations = getLocations(issue, selectedFlowIndex);
  if (
    selectedLocationIndex !== undefined &&
    selectedLocationIndex >= 0 &&
    locations.length >= selectedLocationIndex
  ) {
    return locations[selectedLocationIndex];
  }
  return undefined;
}

export function allLocationsEmpty(
  issue: Pick<Issue, 'flows' | 'secondaryLocations' | 'flowsWithType'>,
  selectedFlowIndex: number | undefined
) {
  return getLocations(issue, selectedFlowIndex).every((location) => !location.msg);
}

export function shouldOpenStandardsFacet(
  openFacets: Dict<boolean>,
  query: Partial<Query>
): boolean {
  return (
    openFacets[STANDARDS] ||
    isFilteredBySecurityIssueTypes(query) ||
    isOneStandardChildFacetOpen(openFacets, query)
  );
}

export function shouldOpenStandardsChildFacet(
  openFacets: Dict<boolean>,
  query: Partial<Query>,
  standardType:
    | SecurityStandard.CWE
    | SecurityStandard.OWASP_TOP10
    | SecurityStandard.OWASP_TOP10_2021
    | SecurityStandard.SANS_TOP25
    | SecurityStandard.SONARSOURCE
): boolean {
  const filter = query[standardType];
  return (
    openFacets[STANDARDS] !== false &&
    (openFacets[standardType] ||
      (standardType !== SecurityStandard.CWE && filter !== undefined && filter.length > 0))
  );
}

export function shouldOpenSonarSourceSecurityFacet(
  openFacets: Dict<boolean>,
  query: Partial<Query>
): boolean {
  // Open it by default if the parent is open, and no other standard is open.
  return (
    shouldOpenStandardsChildFacet(openFacets, query, SecurityStandard.SONARSOURCE) ||
    (shouldOpenStandardsFacet(openFacets, query) && !isOneStandardChildFacetOpen(openFacets, query))
  );
}

function isFilteredBySecurityIssueTypes(query: Partial<Query>): boolean {
  return query.types !== undefined && query.types.includes('VULNERABILITY');
}

function isOneStandardChildFacetOpen(openFacets: Dict<boolean>, query: Partial<Query>): boolean {
  return [
    SecurityStandard.OWASP_TOP10,
    SecurityStandard.SANS_TOP25,
    SecurityStandard.CWE,
    SecurityStandard.SONARSOURCE,
  ].some(
    (
      standardType:
        | SecurityStandard.CWE
        | SecurityStandard.OWASP_TOP10
        | SecurityStandard.OWASP_TOP10_2021
        | SecurityStandard.SANS_TOP25
        | SecurityStandard.SONARSOURCE
    ) => shouldOpenStandardsChildFacet(openFacets, query, standardType)
  );
}
