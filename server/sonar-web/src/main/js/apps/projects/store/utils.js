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
  return num > 0 && num < 6 ? num : null;
};

const getAsLevel = value => {
  if (value === 'ERROR' || value === 'WARN' || value === 'OK') {
    return value;
  }
  return null;
};

const getAsString = value => {
  if (!value) {
    return null;
  }
  return value;
};

const getAsArray = (values, elementGetter) => {
  if (!values) {
    return null;
  }
  return values.split(',').map(elementGetter);
};

const getView = rawValue => (rawValue === 'visualizations' ? rawValue : undefined);

const getVisualization = value => {
  return VISUALIZATIONS.includes(value) ? value : null;
};

export const parseUrlQuery = urlQuery => ({
  gate: getAsLevel(urlQuery['gate']),
  reliability: getAsNumericRating(urlQuery['reliability']),
  security: getAsNumericRating(urlQuery['security']),
  maintainability: getAsNumericRating(urlQuery['maintainability']),
  coverage: getAsNumericRating(urlQuery['coverage']),
  duplications: getAsNumericRating(urlQuery['duplications']),
  size: getAsNumericRating(urlQuery['size']),
  languages: getAsArray(urlQuery['languages'], getAsString),
  tags: getAsArray(urlQuery['tags'], getAsString),
  search: getAsString(urlQuery['search']),
  sort: getAsString(urlQuery['sort']),
  view: getView(urlQuery['view']),
  visualization: getVisualization(urlQuery['visualization'])
});

export const mapMetricToProperty = metricKey => {
  const map = {
    reliability_rating: 'reliability',
    security_rating: 'security',
    sqale_rating: 'maintainability',
    coverage: 'coverage',
    duplicated_lines_density: 'duplications',
    ncloc: 'size',
    alert_status: 'gate',
    languages: 'languages',
    tags: 'tags',
    query: 'search'
  };
  return map[metricKey];
};

export const mapPropertyToMetric = property => {
  const map = {
    reliability: 'reliability_rating',
    security: 'security_rating',
    maintainability: 'sqale_rating',
    coverage: 'coverage',
    duplications: 'duplicated_lines_density',
    size: 'ncloc',
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

const convertCoverage = coverage => {
  switch (coverage) {
    case 1:
      return mapPropertyToMetric('coverage') + ' >= 80';
    case 2:
      return mapPropertyToMetric('coverage') + ' < 80';
    case 3:
      return mapPropertyToMetric('coverage') + ' < 70';
    case 4:
      return mapPropertyToMetric('coverage') + ' < 50';
    case 5:
      return mapPropertyToMetric('coverage') + ' < 30';
    default:
      return '';
  }
};

const convertDuplications = duplications => {
  switch (duplications) {
    case 1:
      return mapPropertyToMetric('duplications') + ' < 3';
    case 2:
      return mapPropertyToMetric('duplications') + ' >= 3';
    case 3:
      return mapPropertyToMetric('duplications') + ' >= 5';
    case 4:
      return mapPropertyToMetric('duplications') + ' >= 10';
    case 5:
      return mapPropertyToMetric('duplications') + ' >= 20';
    default:
      return '';
  }
};

const convertSize = size => {
  switch (size) {
    case 1:
      return mapPropertyToMetric('size') + ' < 1000';
    case 2:
      return mapPropertyToMetric('size') + ' >= 1000';
    case 3:
      return mapPropertyToMetric('size') + ' >= 10000';
    case 4:
      return mapPropertyToMetric('size') + ' >= 100000';
    case 5:
      return mapPropertyToMetric('size') + ' >= 500000';
    default:
      return '';
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

  if (query['coverage'] != null) {
    conditions.push(convertCoverage(query['coverage']));
  }

  if (query['duplications'] != null) {
    conditions.push(convertDuplications(query['duplications']));
  }

  if (query['size'] != null) {
    conditions.push(convertSize(query['size']));
  }

  ['reliability', 'security', 'maintainability'].forEach(property => {
    if (query[property] != null) {
      conditions.push(convertIssuesRating(mapPropertyToMetric(property), query[property]));
    }
  });

  ['languages', 'tags'].forEach(property => {
    const items = query[property];
    if (items != null) {
      if (!Array.isArray(items) || items.length < 2) {
        conditions.push(mapPropertyToMetric(property) + ' = ' + items);
      } else {
        conditions.push(`${mapPropertyToMetric(property)} IN (${items.join(', ')})`);
      }
    }
  });

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
