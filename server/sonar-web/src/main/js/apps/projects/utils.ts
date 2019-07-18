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
import { uniq } from 'lodash';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import { RequestData } from 'sonar-ui-common/helpers/request';
import { Facet, searchProjects } from '../../api/components';
import { getMeasuresForProjects } from '../../api/measures';
import { getOrganizations } from '../../api/organizations';
import { getPeriodValue, isDiffMetric } from '../../helpers/measures';
import { convertToFilter, Query } from './query';

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
  { value: 'reliability' },
  { value: 'security' },
  { value: 'maintainability' },
  { value: 'coverage' },
  { value: 'duplications' },
  { value: 'size' }
];

export const SORTING_LEAK_METRICS: SortingOption[] = [
  { value: 'name' },
  { value: 'analysis_date' },
  { value: 'new_reliability', class: 'projects-leak-sorting-option' },
  { value: 'new_security', class: 'projects-leak-sorting-option' },
  { value: 'new_maintainability', class: 'projects-leak-sorting-option' },
  { value: 'new_coverage', class: 'projects-leak-sorting-option' },
  { value: 'new_duplications', class: 'projects-leak-sorting-option' },
  { value: 'new_lines', class: 'projects-leak-sorting-option' }
];

export const SORTING_SWITCH: T.Dict<string> = {
  analysis_date: 'analysis_date',
  name: 'name',
  reliability: 'new_reliability',
  security: 'new_security',
  maintainability: 'new_maintainability',
  coverage: 'new_coverage',
  duplications: 'new_duplications',
  size: 'new_lines',
  new_reliability: 'reliability',
  new_security: 'security',
  new_maintainability: 'maintainability',
  new_coverage: 'coverage',
  new_duplications: 'duplications',
  new_lines: 'size'
};

export const VIEWS = [{ value: 'overall', label: 'overall' }, { value: 'leak', label: 'new_code' }];

export const VISUALIZATIONS = [
  'risk',
  'reliability',
  'security',
  'maintainability',
  'coverage',
  'duplications'
];

const PAGE_SIZE = 50;
const PAGE_SIZE_VISUALIZATIONS = 99;

const METRICS = [
  'alert_status',
  'bugs',
  'reliability_rating',
  'vulnerabilities',
  'security_rating',
  'code_smells',
  'sqale_rating',
  'duplicated_lines_density',
  'coverage',
  'ncloc',
  'ncloc_language_distribution'
];

const LEAK_METRICS = [
  'alert_status',
  'new_bugs',
  'new_reliability_rating',
  'new_vulnerabilities',
  'new_security_rating',
  'new_code_smells',
  'new_maintainability_rating',
  'new_coverage',
  'new_duplicated_lines_density',
  'new_lines'
];

const METRICS_BY_VISUALIZATION: T.Dict<string[]> = {
  risk: ['reliability_rating', 'security_rating', 'coverage', 'ncloc', 'sqale_index'],
  // x, y, size, color
  reliability: ['ncloc', 'reliability_remediation_effort', 'bugs', 'reliability_rating'],
  security: ['ncloc', 'security_remediation_effort', 'vulnerabilities', 'security_rating'],
  maintainability: ['ncloc', 'sqale_index', 'code_smells', 'sqale_rating'],
  coverage: ['complexity', 'coverage', 'uncovered_lines'],
  duplications: ['ncloc', 'duplicated_lines_density', 'duplicated_blocks']
};

export const FACETS = [
  'reliability_rating',
  'security_rating',
  'sqale_rating',
  'coverage',
  'duplicated_lines_density',
  'ncloc',
  'alert_status',
  'languages',
  'tags'
];

export const LEAK_FACETS = [
  'new_reliability_rating',
  'new_security_rating',
  'new_maintainability_rating',
  'new_coverage',
  'new_duplicated_lines_density',
  'new_lines',
  'alert_status',
  'languages',
  'tags'
];

const REVERSED_FACETS = ['coverage', 'new_coverage'];

export function localizeSorting(sort?: string): string {
  return translate('projects.sort', sort || 'name');
}

export function parseSorting(sort: string): { sortValue: string; sortDesc: boolean } {
  const desc = sort[0] === '-';
  return { sortValue: desc ? sort.substr(1) : sort, sortDesc: desc };
}

export function fetchProjects(
  query: Query,
  isFavorite: boolean,
  organization: T.Organization | undefined,
  pageIndex = 1
) {
  const ps = query.view === 'visualizations' ? PAGE_SIZE_VISUALIZATIONS : PAGE_SIZE;
  const data = convertToQueryData(query, isFavorite, organization && organization.key, {
    p: pageIndex > 1 ? pageIndex : undefined,
    ps,
    facets: defineFacets(query).join(),
    f: 'analysisDate,leakPeriodDate'
  });
  return searchProjects(data)
    .then(response =>
      Promise.all([
        fetchProjectMeasures(response.components, query),
        fetchProjectOrganizations(response.components, organization),
        Promise.resolve(response)
      ])
    )
    .then(([measures, organizations, { components, facets, paging }]) => {
      return {
        facets: getFacetsMap(facets),
        projects: components
          .map(component => {
            const componentMeasures: T.Dict<string> = {};
            measures
              .filter(measure => measure.component === component.key)
              .forEach(measure => {
                const value = isDiffMetric(measure.metric)
                  ? getPeriodValue(measure, 1)
                  : measure.value;
                if (value !== undefined) {
                  componentMeasures[measure.metric] = value;
                }
              });
            return { ...component, measures: componentMeasures };
          })
          .map(component => {
            const organization = organizations.find(o => o.key === component.organization);
            return { ...component, organization };
          }),
        total: paging.total
      };
    });
}

function defineMetrics(query: Query): string[] {
  switch (query.view) {
    case 'visualizations':
      return METRICS_BY_VISUALIZATION[query.visualization || 'risk'];
    case 'leak':
      return LEAK_METRICS;
    default:
      return METRICS;
  }
}

function defineFacets(query: Query): string[] {
  if (query.view === 'leak') {
    return LEAK_FACETS;
  }
  return FACETS;
}

function convertToQueryData(
  query: Query,
  isFavorite: boolean,
  organization?: string,
  defaultData = {}
) {
  const data: RequestData = { ...defaultData, organization };
  const filter = convertToFilter(query, isFavorite);
  const sort = convertToSorting(query);

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

export function fetchProjectMeasures(projects: Array<{ key: string }>, query: Query) {
  if (!projects.length) {
    return Promise.resolve([]);
  }

  const projectKeys = projects.map(project => project.key);
  const metrics = defineMetrics(query);
  return getMeasuresForProjects(projectKeys, metrics);
}

export function fetchProjectOrganizations(
  projects: Array<{ organization: string }>,
  organization: T.Organization | undefined
) {
  if (organization) {
    return Promise.resolve([organization]);
  }
  if (!projects.length) {
    return Promise.resolve([]);
  }

  const organizations = uniq(projects.map(project => project.organization));
  return getOrganizations({ organizations: organizations.join() }).then(r => r.organizations);
}

function mapFacetValues(values: Array<{ val: string; count: number }>) {
  const map: T.Dict<number> = {};
  values.forEach(value => {
    map[value.val] = value.count;
  });
  return map;
}

function getFacetsMap(facets: Facet[]) {
  const map: T.Dict<T.Dict<number>> = {};
  facets.forEach(facet => {
    const property = mapMetricToProperty(facet.property);
    const { values } = facet;
    if (REVERSED_FACETS.includes(property)) {
      values.reverse();
    }
    map[property] = mapFacetValues(values);
  });
  return map;
}

function mapPropertyToMetric(property?: string) {
  const map: T.Dict<string> = {
    analysis_date: 'analysisDate',
    reliability: 'reliability_rating',
    new_reliability: 'new_reliability_rating',
    security: 'security_rating',
    new_security: 'new_security_rating',
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
    search: 'query'
  };
  return property && map[property];
}

function convertToSorting({ sort }: Query): { s?: string; asc?: boolean } {
  if (sort && sort[0] === '-') {
    return { s: mapPropertyToMetric(sort.substr(1)), asc: false };
  }
  return { s: mapPropertyToMetric(sort) };
}

function mapMetricToProperty(metricKey: string) {
  const map: T.Dict<string> = {
    analysisDate: 'analysis_date',
    reliability_rating: 'reliability',
    new_reliability_rating: 'new_reliability',
    security_rating: 'security',
    new_security_rating: 'new_security',
    sqale_rating: 'maintainability',
    new_maintainability_rating: 'new_maintainability',
    coverage: 'coverage',
    new_coverage: 'new_coverage',
    duplicated_lines_density: 'duplications',
    new_duplicated_lines_density: 'new_duplications',
    ncloc: 'size',
    new_lines: 'new_lines',
    alert_status: 'gate',
    languages: 'languages',
    tags: 'tags',
    query: 'search'
  };
  return map[metricKey];
}

const ONE_MINUTE = 60000;
const ONE_HOUR = 60 * ONE_MINUTE;
const ONE_DAY = 24 * ONE_HOUR;
const ONE_MONTH = 30 * ONE_DAY;
const ONE_YEAR = 12 * ONE_MONTH;

function format(periods: Array<{ value: number; label: string }>) {
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
    { value: minutes, label: 'duration.minutes' }
  ]);
}
