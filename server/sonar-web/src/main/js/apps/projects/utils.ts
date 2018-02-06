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
import { sumBy, uniq } from 'lodash';
import { Query, convertToFilter } from './query';
import { translate } from '../../helpers/l10n';
import { RequestData } from '../../helpers/request';
import { getOrganizations } from '../../api/organizations';
import { searchProjects, Facet } from '../../api/components';
import { getMeasuresForProjects } from '../../api/measures';
import { isDiffMetric, getPeriodValue } from '../../helpers/measures';

interface SortingOption {
  class?: string;
  value: string;
}

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

export const SORTING_SWITCH: { [x: string]: string } = {
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

export const VIEWS = ['overall', 'leak'];

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

const METRICS_BY_VISUALIZATION: { [x: string]: string[] } = {
  risk: ['reliability_rating', 'security_rating', 'coverage', 'ncloc', 'sqale_index'],
  // x, y, size, color
  reliability: ['ncloc', 'reliability_remediation_effort', 'bugs', 'reliability_rating'],
  security: ['ncloc', 'security_remediation_effort', 'vulnerabilities', 'security_rating'],
  maintainability: ['ncloc', 'sqale_index', 'code_smells', 'sqale_rating'],
  coverage: ['complexity', 'coverage', 'uncovered_lines'],
  duplications: ['ncloc', 'duplicated_lines_density', 'duplicated_blocks']
};

const FACETS = [
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

const LEAK_FACETS = [
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

const CUMULATIVE_FACETS = [
  'reliability',
  'new_reliability',
  'security',
  'new_security',
  'maintainability',
  'new_maintainability',
  'coverage',
  'new_coverage',
  'duplications',
  'new_duplications',
  'size',
  'new_lines'
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
  organization?: string,
  pageIndex = 1
) {
  const ps = query.view === 'visualizations' ? PAGE_SIZE_VISUALIZATIONS : PAGE_SIZE;
  const data = convertToQueryData(query, isFavorite, organization, {
    p: pageIndex > 1 ? pageIndex : undefined,
    ps,
    facets: defineFacets(query).join(),
    f: 'analysisDate,leakPeriodDate'
  });
  return searchProjects(data).then(({ components, facets, paging }) => {
    return Promise.all([
      fetchProjectMeasures(components, query),
      fetchProjectOrganizations(components)
    ]).then(([measures, organizations]) => {
      return {
        facets: getFacetsMap(facets),
        projects: components
          .map(component => {
            const componentMeasures: { [key: string]: string } = {};
            measures.filter(measure => measure.component === component.key).forEach(measure => {
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
  const sort = convertToSorting(query as any);

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

function fetchProjectMeasures(projects: Array<{ key: string }>, query: Query) {
  if (!projects.length) {
    return Promise.resolve([]);
  }

  const projectKeys = projects.map(project => project.key);
  const metrics = defineMetrics(query);
  return getMeasuresForProjects(projectKeys, metrics);
}

function fetchProjectOrganizations(projects: Array<{ organization: string }>) {
  if (!projects.length) {
    return Promise.resolve([]);
  }

  const organizations = uniq(projects.map(project => project.organization));
  return getOrganizations({ organizations: organizations.join() }).then(r => r.organizations);
}

function mapFacetValues(values: Array<{ val: string; count: number }>) {
  const map: { [value: string]: number } = {};
  values.forEach(value => {
    map[value.val] = value.count;
  });
  return map;
}

export function cumulativeMapFacetValues(values: Array<{ val: string; count: number }>) {
  const noDataVal = values.find(value => value.val === 'NO_DATA');
  const filteredValues = noDataVal ? values.filter(value => value.val !== 'NO_DATA') : values;

  let sum = sumBy(filteredValues, value => value.count);
  const map: { [value: string]: number } = {};
  filteredValues.forEach((value, index) => {
    map[value.val] = index > 0 && index < values.length - 1 ? sum : value.count;
    sum -= value.count;
  });

  if (noDataVal) {
    map[noDataVal.val] = noDataVal.count;
  }
  return map;
}

function getFacetsMap(facets: Facet[]) {
  const map: { [property: string]: { [value: string]: number } } = {};
  facets.forEach(facet => {
    const property = mapMetricToProperty(facet.property);
    const { values } = facet;
    if (REVERSED_FACETS.includes(property)) {
      values.reverse();
    }
    map[property] = CUMULATIVE_FACETS.includes(property)
      ? cumulativeMapFacetValues(values)
      : mapFacetValues(values);
  });
  return map;
}

function mapPropertyToMetric(property?: string) {
  const map: { [property: string]: string } = {
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
  const map: { [metric: string]: string } = {
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
