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

import { invert } from 'lodash';
import { MetricKey } from '~sonar-aligned/types/metrics';
import { Facet, getScannableProjects, searchProjects } from '../../api/components';
import { translate, translateWithParameters } from '../../helpers/l10n';
import { RequestData } from '../../helpers/request';
import { Dict } from '../../types/types';
import { Query, convertToFilter } from './query';

interface SortingOption {
  class?: string;
  value: string;
}

export const PROJECTS_DEFAULT_FILTER = 'sonarqube.projects.default';
export const PROJECTS_FAVORITE = 'favorite';
export const PROJECTS_ALL = 'all';

export const SORTING_METRICS: SortingOption[] = [
  { value: 'name' },
  { value: 'analysis_date' },
  { value: 'creation_date' },
  { value: 'reliability' },
  { value: 'security' },
  { value: 'security_review' },
  { value: 'maintainability' },
  { value: 'coverage' },
  { value: 'duplications' },
  { value: 'size' },
];

export const SORTING_LEAK_METRICS: SortingOption[] = [
  { value: 'name' },
  { value: 'analysis_date' },
  { value: 'creation_date' },
  { value: 'new_reliability', class: 'projects-leak-sorting-option' },
  { value: 'new_security', class: 'projects-leak-sorting-option' },
  { value: 'new_security_review', class: 'projects-leak-sorting-option' },
  { value: 'new_maintainability', class: 'projects-leak-sorting-option' },
  { value: 'new_coverage', class: 'projects-leak-sorting-option' },
  { value: 'new_duplications', class: 'projects-leak-sorting-option' },
  { value: 'new_lines', class: 'projects-leak-sorting-option' },
];

export const SORTING_SWITCH: Dict<string> = {
  analysis_date: 'analysis_date',
  name: 'name',
  reliability: 'new_reliability',
  security: 'new_security',
  security_review: 'new_security_review',
  maintainability: 'new_maintainability',
  coverage: 'new_coverage',
  duplications: 'new_duplications',
  size: 'new_lines',
  new_reliability: 'reliability',
  new_security: 'security',
  new_security_review: 'security_review',
  new_maintainability: 'maintainability',
  new_coverage: 'coverage',
  new_duplications: 'duplications',
  new_lines: 'size',
};

export const VIEWS = [
  { value: 'overall', label: 'overall' },
  { value: 'leak', label: 'new_code' },
];

const PAGE_SIZE = 50;

export const METRICS = [
  MetricKey.alert_status,
  MetricKey.reliability_issues,
  MetricKey.bugs,
  MetricKey.reliability_rating,
  MetricKey.software_quality_reliability_rating,
  MetricKey.security_issues,
  MetricKey.vulnerabilities,
  MetricKey.security_rating,
  MetricKey.software_quality_security_rating,
  MetricKey.maintainability_issues,
  MetricKey.code_smells,
  MetricKey.sqale_rating,
  MetricKey.software_quality_maintainability_rating,
  MetricKey.security_hotspots_reviewed,
  MetricKey.security_review_rating,
  MetricKey.duplicated_lines_density,
  MetricKey.coverage,
  MetricKey.ncloc,
  MetricKey.ncloc_language_distribution,
  MetricKey.projects,
];

export const LEAK_METRICS = [
  MetricKey.alert_status,
  MetricKey.new_violations,
  MetricKey.new_security_hotspots_reviewed,
  MetricKey.new_security_review_rating,
  MetricKey.new_coverage,
  MetricKey.new_duplicated_lines_density,
  MetricKey.new_lines,
  MetricKey.projects,
];

export const LEGACY_FACETS = [
  MetricKey.reliability_rating,
  MetricKey.security_rating,
  MetricKey.security_review_rating,
  MetricKey.sqale_rating,
  MetricKey.coverage,
  MetricKey.duplicated_lines_density,
  MetricKey.ncloc,
  MetricKey.alert_status,
  'languages',
  'tags',
  'qualifier',
];

export const FACETS = [
  MetricKey.software_quality_reliability_rating,
  MetricKey.software_quality_security_rating,
  MetricKey.software_quality_maintainability_rating,
  MetricKey.security_review_rating,
  MetricKey.coverage,
  MetricKey.duplicated_lines_density,
  MetricKey.ncloc,
  MetricKey.alert_status,
  'languages',
  'tags',
  'qualifier',
];

export const LEGACY_LEAK_FACETS = [
  MetricKey.new_reliability_rating,
  MetricKey.new_security_rating,
  MetricKey.new_security_review_rating,
  MetricKey.new_maintainability_rating,
  MetricKey.new_coverage,
  MetricKey.new_duplicated_lines_density,
  MetricKey.new_lines,
  MetricKey.alert_status,
  'languages',
  'tags',
  'qualifier',
];

export const LEAK_FACETS = [
  MetricKey.new_software_quality_reliability_rating,
  MetricKey.new_software_quality_security_rating,
  MetricKey.new_software_quality_maintainability_rating,
  MetricKey.new_security_review_rating,
  MetricKey.new_coverage,
  MetricKey.new_duplicated_lines_density,
  MetricKey.new_lines,
  MetricKey.alert_status,
  'languages',
  'tags',
  'qualifier',
];

const REVERSED_FACETS = ['coverage', 'new_coverage'];
let scannableProjectsCached: { key: string; name: string }[] | null = null;

export function localizeSorting(sort?: string): string {
  return translate('projects.sort', sort ?? 'name');
}

export function parseSorting(sort: string): { sortDesc: boolean; sortValue: string } {
  const desc = sort.startsWith('-');

  return { sortValue: desc ? sort.substring(1) : sort, sortDesc: desc };
}

export async function fetchScannableProjects() {
  if (scannableProjectsCached) {
    return Promise.resolve({ scannableProjects: scannableProjectsCached });
  }

  const response = await getScannableProjects().then(({ projects }) => {
    scannableProjectsCached = projects;
    return projects;
  });

  return { scannableProjects: response };
}

export function fetchProjects({
  isFavorite,
  query,
  pageIndex = 1,
  isStandardMode,
}: {
  isFavorite: boolean;
  isStandardMode: boolean;
  pageIndex?: number;
  query: Query;
}) {
  const ps = PAGE_SIZE;

  const data = convertToQueryData(query, isFavorite, isStandardMode, {
    p: pageIndex > 1 ? pageIndex : undefined,
    ps,
    facets: defineFacets(query, isStandardMode).join(),
    f: 'analysisDate,leakPeriodDate',
  });

  return searchProjects(data)
    .then((response) => Promise.all([Promise.resolve(response), fetchScannableProjects()]))
    .then(([{ components, facets, paging }, { scannableProjects }]) => {
      return {
        facets: getFacetsMap(facets, isStandardMode),
        projects: components.map((component) => ({
          ...component,
          isScannable: scannableProjects.find((p) => p.key === component.key) !== undefined,
        })),
        total: paging.total,
      };
    });
}

export function defineMetrics(query: Query): string[] {
  if (query.view === 'leak') {
    return LEAK_METRICS;
  }

  return METRICS;
}

function defineFacets(query: Query, isStandardMode: boolean): string[] {
  if (query.view === 'leak') {
    return isStandardMode ? LEGACY_LEAK_FACETS : LEAK_FACETS;
  }

  return isStandardMode ? LEGACY_FACETS : FACETS;
}

export function convertToQueryData(
  query: Query,
  isFavorite: boolean,
  isStandardMode: boolean,
  defaultData = {},
) {
  const data: RequestData = { ...defaultData };
  const filter = convertToFilter(query, isFavorite, isStandardMode);
  const sort = convertToSorting(query, isStandardMode);

  if (filter) {
    data.filter = filter;
  }

  if (sort.s) {
    data.s = sort.s;
  }

  if (sort.asc !== undefined) {
    data.asc = sort.asc;
  }

  return data;
}

function mapFacetValues(values: Array<{ count: number; val: string }>) {
  const map: Dict<number> = {};

  values.forEach((value) => {
    map[value.val] = value.count;
  });

  return map;
}

export const propertyToMetricMapLegacy: Dict<string | undefined> = {
  analysis_date: 'analysisDate',
  reliability: 'reliability_rating',
  new_reliability: 'new_reliability_rating',
  security: 'security_rating',
  new_security: 'new_security_rating',
  security_review: 'security_review_rating',
  new_security_review: 'new_security_review_rating',
  maintainability: 'sqale_rating',
  new_maintainability: 'new_maintainability_rating',
  coverage: 'coverage',
  new_coverage: 'new_coverage',
  duplications: 'duplicated_lines_density',
  new_duplications: 'new_duplicated_lines_density',
  size: 'ncloc',
  new_lines: 'new_lines',
  gate: 'alert_status',
  languages: 'languages',
  tags: 'tags',
  search: 'query',
  qualifier: 'qualifier',
  creation_date: 'creationDate',
};

export const propertyToMetricMap: Dict<string | undefined> = {
  ...propertyToMetricMapLegacy,
  reliability: 'software_quality_reliability_rating',
  new_reliability: 'new_software_quality_reliability_rating',
  security: 'software_quality_security_rating',
  new_security: 'new_software_quality_security_rating',
  maintainability: 'software_quality_maintainability_rating',
  new_maintainability: 'new_software_quality_maintainability_rating',
};

function getFacetsMap(facets: Facet[], isStandardMode: boolean) {
  const map: Dict<Dict<number>> = {};

  facets.forEach((facet) => {
    const property = invert(isStandardMode ? propertyToMetricMapLegacy : propertyToMetricMap)[
      facet.property
    ];
    const { values } = facet;

    if (REVERSED_FACETS.includes(property)) {
      values.reverse();
    }

    map[property] = mapFacetValues(values);
  });

  return map;
}

export function convertToSorting(
  { sort }: Query,
  isStandardMode: boolean,
): { asc?: boolean; s?: string } {
  if (sort?.startsWith('-')) {
    return {
      s: (isStandardMode ? propertyToMetricMapLegacy : propertyToMetricMap)[sort.substring(1)],
      asc: false,
    };
  }

  return { s: (isStandardMode ? propertyToMetricMapLegacy : propertyToMetricMap)[sort ?? ''] };
}

const ONE_MINUTE = 60000;
const ONE_HOUR = 60 * ONE_MINUTE;
const ONE_DAY = 24 * ONE_HOUR;
const ONE_MONTH = 30 * ONE_DAY;
const ONE_YEAR = 12 * ONE_MONTH;

function format(periods: Array<{ label: string; value: number }>) {
  let result = '';
  let count = 0;
  let lastId = -1;

  for (let i = 0; i < periods.length && count < 2; i++) {
    if (periods[i].value > 0) {
      count++;

      if (lastId < 0 || lastId + 1 === i) {
        lastId = i;
        result += translateWithParameters(periods[i].label, periods[i].value) + ' ';
      }
    }
  }

  return result;
}

export function formatDuration(ms: number) {
  if (ms < ONE_MINUTE) {
    return translate('duration.seconds');
  }

  const years = Math.floor(ms / ONE_YEAR);
  ms -= years * ONE_YEAR;

  const months = Math.floor(ms / ONE_MONTH);
  ms -= months * ONE_MONTH;

  const days = Math.floor(ms / ONE_DAY);
  ms -= days * ONE_DAY;

  const hours = Math.floor(ms / ONE_HOUR);
  ms -= hours * ONE_HOUR;

  const minutes = Math.floor(ms / ONE_MINUTE);

  return format([
    { value: years, label: 'duration.years' },
    { value: months, label: 'duration.months' },
    { value: days, label: 'duration.days' },
    { value: hours, label: 'duration.hours' },
    { value: minutes, label: 'duration.minutes' },
  ]);
}
