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

import { intersection, isArray, uniq } from 'lodash';
import { formatMeasure } from '~sonar-aligned/helpers/measures';
import { MetricType } from '~sonar-aligned/types/metrics';
import { RawQuery } from '~sonar-aligned/types/router';
import { getUsers } from '../../api/users';
import { DEFAULT_ISSUES_QUERY } from '../../components/shared/utils';
import {
  cleanQuery,
  parseAsArray,
  parseAsBoolean,
  parseAsDate,
  parseAsOptionalBoolean,
  parseAsString,
  queriesEqual,
  serializeDateShort,
  serializeOptionalBoolean,
  serializeString,
  serializeStringArray,
} from '../../helpers/query';
import { get, save } from '../../helpers/storage';
import { isDefined } from '../../helpers/types';
import {
  CleanCodeAttributeCategory,
  SoftwareImpactSeverity,
  SoftwareQuality,
} from '../../types/clean-code-taxonomy';
import {
  Facet,
  IssueDeprecatedStatus,
  IssueResolution,
  IssueStatus,
  RawFacet,
} from '../../types/issues';
import { SecurityStandard } from '../../types/security';
import { Dict, Flow, FlowType, Issue, Paging } from '../../types/types';
import { RestUser } from '../../types/users';
import { searchMembers } from '../../api/organizations';

const OWASP_ASVS_4_0 = 'owaspAsvs-4.0';

export interface Query {
  [OWASP_ASVS_4_0]: string[];
  assigned: boolean;
  assignees: string[];
  author: string[];
  casa: string[];
  cleanCodeAttributeCategories: CleanCodeAttributeCategory[];
  codeVariants: string[];
  createdAfter: Date | undefined;
  createdAt: string;
  createdBefore: Date | undefined;
  createdInLast: string;
  cwe: string[];
  directories: string[];
  files: string[];
  fixedInPullRequest: string;
  impactSeverities: SoftwareImpactSeverity[];
  impactSoftwareQualities: SoftwareQuality[];
  inNewCodePeriod: boolean;
  issueStatuses: IssueStatus[];
  issues: string[];
  languages: string[];
  owaspAsvsLevel: string;
  owaspTop10: string[];
  'owaspTop10-2021': string[];
  'pciDss-3.2': string[];
  'pciDss-4.0': string[];
  prioritizedRule?: boolean;
  projects: string[];
  resolved?: boolean;
  rules: string[];
  scopes: string[];
  severities: string[];
  sonarsourceSecurity: string[];
  sort: string;
  'stig-ASD_V5R3': string[];
  tags: string[];
  types: string[];
}

export const STANDARDS = 'standards';

// allow sorting by CREATION_DATE only
const parseAsSort = (sort: string) => (sort === 'CREATION_DATE' ? 'CREATION_DATE' : '');
const ISSUES_DEFAULT = 'sonarqube.issues.default';

export function parseQuery(query: RawQuery, needIssueSync = false): Query {
  return {
    assigned: parseAsBoolean(query.assigned),
    assignees: parseAsArray(query.assignees, parseAsString),
    author: isArray(query.author) ? query.author : [query.author].filter(isDefined),
    cleanCodeAttributeCategories: parseAsArray<CleanCodeAttributeCategory>(
      query.cleanCodeAttributeCategories,
      parseAsString,
    ),
    createdAfter: parseAsDate(query.createdAfter),
    createdAt: parseAsString(query.createdAt),
    createdBefore: parseAsDate(query.createdBefore),
    createdInLast: parseAsString(query.createdInLast),
    cwe: parseAsArray(query.cwe, parseAsString),
    directories: parseAsArray(query.directories, parseAsString),
    files: parseAsArray(query.files, parseAsString),
    impactSeverities: parseAsArray<SoftwareImpactSeverity>(query.impactSeverities, parseAsString),
    impactSoftwareQualities: parseAsArray<SoftwareQuality>(
      query.impactSoftwareQualities,
      parseAsString,
    ),
    'stig-ASD_V5R3': parseAsArray(query['stig-ASD_V5R3'], parseAsString),
    inNewCodePeriod: parseAsBoolean(query.inNewCodePeriod, false),
    issues: parseAsArray(query.issues, parseAsString),
    languages: parseAsArray(query.languages, parseAsString),
    owaspTop10: parseAsArray(query.owaspTop10, parseAsString),
    'owaspTop10-2021': parseAsArray(query['owaspTop10-2021'], parseAsString),
    'pciDss-3.2': parseAsArray(query['pciDss-3.2'], parseAsString),
    'pciDss-4.0': parseAsArray(query['pciDss-4.0'], parseAsString),
    casa: parseAsArray(query['casa'], parseAsString),
    [OWASP_ASVS_4_0]: parseAsArray(query[OWASP_ASVS_4_0], parseAsString),
    owaspAsvsLevel: parseAsString(query['owaspAsvsLevel']),
    projects: parseAsArray(query.projects, parseAsString),
    rules: parseAsArray(query.rules, parseAsString),
    scopes: parseAsArray(query.scopes, parseAsString),
    severities: parseAsArray(query.severities, parseAsString),
    sonarsourceSecurity: parseAsArray(query.sonarsourceSecurity, parseAsString),
    sort: parseAsSort(query.s),
    issueStatuses: parseIssueStatuses(query),
    tags: parseAsArray(query.tags, parseAsString),
    types: parseAsArray(query.types, parseAsString),
    codeVariants: parseAsArray(query.codeVariants, parseAsString),
    fixedInPullRequest: parseAsString(query.fixedInPullRequest),
    prioritizedRule: parseAsOptionalBoolean(query.prioritizedRule),
    // While reindexing, we need to use resolved param for issues/list endpoint
    // False is used to show unresolved issues only
    resolved: needIssueSync ? false : undefined,
  };
}

function parseIssueStatuses(query: RawQuery) {
  let result: Array<IssueStatus> = [];

  if (query.issueStatuses) {
    return parseAsArray<IssueStatus>(query.issueStatuses, parseAsString);
  }

  const deprecatedStatusesMap = {
    [IssueDeprecatedStatus.Open]: [IssueStatus.Open],
    [IssueDeprecatedStatus.Confirmed]: [IssueStatus.Confirmed],
    [IssueDeprecatedStatus.Reopened]: [IssueStatus.Open],
    [IssueDeprecatedStatus.Resolved]: [
      IssueStatus.Fixed,
      IssueStatus.Accepted,
      IssueStatus.FalsePositive,
    ],
    [IssueDeprecatedStatus.Closed]: [IssueStatus.Fixed],
  };
  const deprecatedResolutionsMap = {
    [IssueResolution.FalsePositive]: [IssueStatus.FalsePositive],
    [IssueResolution.WontFix]: [IssueStatus.Accepted],
    [IssueResolution.Fixed]: [IssueStatus.Fixed],
    [IssueResolution.Removed]: [IssueStatus.Fixed],
    [IssueResolution.Unresolved]: [IssueStatus.Open, IssueStatus.Confirmed],
  };

  const issuesStatusesFromDeprecatedStatuses = parseAsArray<IssueDeprecatedStatus>(
    query.statuses,
    parseAsString,
  )
    .map((status) => deprecatedStatusesMap[status])
    .filter(Boolean)
    .flat();
  const issueStatusesFromResolutions = parseAsArray<IssueResolution>(
    query.resolutions,
    parseAsString,
  )
    .map((status) => deprecatedResolutionsMap[status])
    .filter(Boolean)
    .flat();

  const intesectedIssueStatuses = intersection(
    issuesStatusesFromDeprecatedStatuses,
    issueStatusesFromResolutions,
  );
  result = intesectedIssueStatuses.length
    ? intesectedIssueStatuses
    : issueStatusesFromResolutions.concat(issuesStatusesFromDeprecatedStatuses);

  if (
    query.resolved === 'false' &&
    [IssueStatus.Open, IssueStatus.Confirmed].every((status) => !result.includes(status))
  ) {
    result = result.concat(
      parseAsArray<IssueStatus>(DEFAULT_ISSUES_QUERY.issueStatuses, parseAsString),
    );
  }

  return uniq(result);
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
    cleanCodeAttributeCategories: serializeStringArray(query.cleanCodeAttributeCategories),
    createdAfter: serializeDateShort(query.createdAfter),
    createdAt: serializeString(query.createdAt),
    createdBefore: serializeDateShort(query.createdBefore),
    createdInLast: serializeString(query.createdInLast),
    cwe: serializeStringArray(query.cwe),
    directories: serializeStringArray(query.directories),
    files: serializeStringArray(query.files),
    fixedInPullRequest: serializeString(query.fixedInPullRequest),
    issues: serializeStringArray(query.issues),
    languages: serializeStringArray(query.languages),
    owaspTop10: serializeStringArray(query.owaspTop10),
    'owaspTop10-2021': serializeStringArray(query['owaspTop10-2021']),
    'pciDss-3.2': serializeStringArray(query['pciDss-3.2']),
    casa: serializeStringArray(query['casa']),
    'stig-ASD_V5R3': serializeStringArray(query['stig-ASD_V5R3']),
    'pciDss-4.0': serializeStringArray(query['pciDss-4.0']),
    [OWASP_ASVS_4_0]: serializeStringArray(query[OWASP_ASVS_4_0]),
    owaspAsvsLevel: serializeString(query['owaspAsvsLevel']),
    projects: serializeStringArray(query.projects),
    rules: serializeStringArray(query.rules),
    s: serializeString(query.sort),
    scopes: serializeStringArray(query.scopes),
    severities: serializeStringArray(query.severities),
    impactSeverities: serializeStringArray(query.impactSeverities),
    impactSoftwareQualities: serializeStringArray(query.impactSoftwareQualities),
    inNewCodePeriod: query.inNewCodePeriod ? 'true' : undefined,
    sonarsourceSecurity: serializeStringArray(query.sonarsourceSecurity),
    issueStatuses: serializeStringArray(query.issueStatuses),
    tags: serializeStringArray(query.tags),
    types: serializeStringArray(query.types),
    codeVariants: serializeStringArray(query.codeVariants),
    resolved: serializeOptionalBoolean(query.resolved),
    prioritizedRule: serializeOptionalBoolean(query.prioritizedRule),
  };

  return cleanQuery(filter);
}

export const areQueriesEqual = (a: RawQuery, b: RawQuery) =>
  queriesEqual(parseQuery(a), parseQuery(b));

export function parseFacets(facets?: RawFacet[]): Dict<Facet> {
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
  return stat && formatMeasure(stat, MetricType.ShortInteger);
}

export const searchAssignees = (
  query: string,
  organization: string | undefined,
  page = 1,
): Promise<{ paging: Paging; results: RestUser[] }> => {
  return organization
    ? searchMembers({ organization, p: page, ps: 50, q: query }).then(({ paging, users }) => ({
      paging,
      results: users
    }))
    : getUsers<RestUser>({ pageIndex: page, q: query }).then(({ page, users }) => ({
    paging: page,
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

export function getTypedFlows(flows: Flow[]) {
  return flows.map((flow) => ({
    ...flow,
    locations:
      flow.type === FlowType.EXECUTION ? [...(flow.locations ?? [])].reverse() : flow.locations,
  }));
}

export function getLocations(
  {
    flows,
    secondaryLocations,
    flowsWithType,
  }: Pick<Issue, 'flows' | 'secondaryLocations' | 'flowsWithType'>,
  selectedFlowIndex: number | undefined,
) {
  if (secondaryLocations.length > 0) {
    return secondaryLocations;
  }

  if (selectedFlowIndex !== undefined) {
    if (flows[selectedFlowIndex] !== undefined) {
      return flows[selectedFlowIndex];
    }

    if (flowsWithType[selectedFlowIndex] !== undefined) {
      return getTypedFlows(flowsWithType)[selectedFlowIndex].locations || [];
    }

    return [];
  }

  return [];
}

export function getSelectedLocation(
  issue: Pick<Issue, 'flows' | 'secondaryLocations' | 'flowsWithType'>,
  selectedFlowIndex: number | undefined,
  selectedLocationIndex: number | undefined,
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
  selectedFlowIndex: number | undefined,
) {
  return getLocations(issue, selectedFlowIndex).every((location) => !location.msg);
}

export function shouldOpenStandardsFacet(
  openFacets: Dict<boolean>,
  query: Partial<Query>,
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
    | SecurityStandard.SONARSOURCE,
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
  query: Partial<Query>,
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
  return [SecurityStandard.OWASP_TOP10, SecurityStandard.CWE, SecurityStandard.SONARSOURCE].some(
    (
      standardType:
        | SecurityStandard.CWE
        | SecurityStandard.OWASP_TOP10
        | SecurityStandard.OWASP_TOP10_2021
        | SecurityStandard.SONARSOURCE,
    ) => shouldOpenStandardsChildFacet(openFacets, query, standardType),
  );
}
