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
import { VISUALIZATIONS } from '../utils';

const getAsNumericRating = value => {
  if (value === '' || value == null || isNaN(value)) {
    return null;
  }
  const num = Number(value);
  return num > 0 && num < 7 ? num : null;
};

const getAsLevel = value => {
  if (value === 'ERROR' || value === 'WARN' || value === 'OK') {
    return value;
  }
  return null;
};

// TODO Maybe use parseAsString form helpers/query
const getAsString = value => {
  if (!value) {
    return null;
  }
  return value;
};

// TODO Maybe move it to helpers/query
const getAsArray = (values, elementGetter) => {
  if (!values) {
    return null;
  }
  return values.split(',').map(elementGetter);
};

const getView = rawValue => (rawValue === 'overall' ? undefined : rawValue);

const getVisualization = value => {
  return VISUALIZATIONS.includes(value) ? value : null;
};

export const parseUrlQuery = urlQuery => ({
  gate: getAsLevel(urlQuery['gate']),
  reliability: getAsNumericRating(urlQuery['reliability']),
  new_reliability: getAsNumericRating(urlQuery['new_reliability']),
  security: getAsNumericRating(urlQuery['security']),
  new_security: getAsNumericRating(urlQuery['new_security']),
  maintainability: getAsNumericRating(urlQuery['maintainability']),
  new_maintainability: getAsNumericRating(urlQuery['new_maintainability']),
  coverage: getAsNumericRating(urlQuery['coverage']),
  new_coverage: getAsNumericRating(urlQuery['new_coverage']),
  duplications: getAsNumericRating(urlQuery['duplications']),
  new_duplications: getAsNumericRating(urlQuery['new_duplications']),
  size: getAsNumericRating(urlQuery['size']),
  new_lines: getAsNumericRating(urlQuery['new_lines']),
  languages: getAsArray(urlQuery['languages'], getAsString),
  tags: getAsArray(urlQuery['tags'], getAsString),
  search: getAsString(urlQuery['search']),
  sort: getAsString(urlQuery['sort']),
  view: getView(urlQuery['view']),
  visualization: getVisualization(urlQuery['visualization'])
});

export const mapMetricToProperty = metricKey => {
  const map = {
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
};

export const mapPropertyToMetric = property => {
  const map = {
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
  return map[property];
};

const convertIssuesRating = (metric, rating) => {
  if (rating > 1 && rating < 5) {
    return `${metric} >= ${rating}`;
  } else {
    return `${metric} = ${rating}`;
  }
};

const convertCoverage = (metric, coverage) => {
  switch (coverage) {
    case 1:
      return metric + ' >= 80';
    case 2:
      return metric + ' < 80';
    case 3:
      return metric + ' < 70';
    case 4:
      return metric + ' < 50';
    case 5:
      return metric + ' < 30';
    case 6:
      return metric + '= NO_DATA';
    default:
      return '';
  }
};

const convertDuplications = (metric, duplications) => {
  switch (duplications) {
    case 1:
      return metric + ' < 3';
    case 2:
      return metric + ' >= 3';
    case 3:
      return metric + ' >= 5';
    case 4:
      return metric + ' >= 10';
    case 5:
      return metric + ' >= 20';
    case 6:
      return metric + '= NO_DATA';
    default:
      return '';
  }
};

const convertSize = (metric, size) => {
  switch (size) {
    case 1:
      return metric + ' < 1000';
    case 2:
      return metric + ' >= 1000';
    case 3:
      return metric + ' >= 10000';
    case 4:
      return metric + ' >= 100000';
    case 5:
      return metric + ' >= 500000';
    default:
      return '';
  }
};

const convertArrayMetric = (metric, items) => {
  if (!Array.isArray(items) || items.length < 2) {
    return metric + ' = ' + items;
  }
  return `${metric} IN (${items.join(', ')})`;
};

const pushMetricToArray = (query, property, conditionsArray, convertFunction) => {
  if (query[property] != null) {
    conditionsArray.push(convertFunction(mapPropertyToMetric(property), query[property]));
  }
};

export const convertToFilter = (query, isFavorite) => {
  const conditions = [];

  if (isFavorite) {
    conditions.push('isFavorite');
  }

  if (query['gate'] != null) {
    conditions.push(mapPropertyToMetric('gate') + ' = ' + query['gate']);
  }

  ['coverage', 'new_coverage'].forEach(property =>
    pushMetricToArray(query, property, conditions, convertCoverage)
  );

  ['duplications', 'new_duplications'].forEach(property =>
    pushMetricToArray(query, property, conditions, convertDuplications)
  );

  ['size', 'new_lines'].forEach(property =>
    pushMetricToArray(query, property, conditions, convertSize)
  );

  [
    'reliability',
    'security',
    'maintainability',
    'new_reliability',
    'new_security',
    'new_maintainability'
  ].forEach(property => pushMetricToArray(query, property, conditions, convertIssuesRating));

  ['languages', 'tags'].forEach(property =>
    pushMetricToArray(query, property, conditions, convertArrayMetric)
  );

  if (query['search'] != null) {
    conditions.push(`${mapPropertyToMetric('search')} = "${query['search']}"`);
  }

  return conditions.join(' and ');
};

export const convertToSorting = ({ sort }) => {
  if (sort && sort[0] === '-') {
    return { s: mapPropertyToMetric(sort.substr(1)), asc: false };
  }
  return { s: mapPropertyToMetric(sort) };
};

export const convertToQueryData = (query, isFavorite, organization, defaultData = {}) => {
  const data = { ...defaultData };
  const filter = convertToFilter(query, isFavorite);
  const sort = convertToSorting(query);

  if (filter) {
    data.filter = filter;
  }
  if (sort.s) {
    data.s = sort.s;
  }
  if (sort.hasOwnProperty('asc')) {
    data.asc = sort.asc;
  }
  if (organization) {
    data.organization = organization.key;
  }
  return data;
};
