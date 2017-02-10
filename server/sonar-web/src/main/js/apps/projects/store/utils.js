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
const getAsNumericRating = value => {
  if (value === '' || value == null || isNaN(value)) {
    return null;
  }
  const num = Number(value);
  return (num > 0 && num < 6) ? num : null;
};

const getAsLevel = value => {
  if (value === 'ERROR' || value === 'WARN' || value === 'OK') {
    return value;
  }
  return null;
};

export const parseUrlQuery = urlQuery => ({
  'gate': getAsLevel(urlQuery['gate']),
  'reliability': getAsNumericRating(urlQuery['reliability']),
  'security': getAsNumericRating(urlQuery['security']),
  'maintainability': getAsNumericRating(urlQuery['maintainability']),
  'coverage': getAsNumericRating(urlQuery['coverage']),
  'duplications': getAsNumericRating(urlQuery['duplications']),
  'size': getAsNumericRating(urlQuery['size'])
});

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
      return 'coverage >= 80';
    case 2:
      return 'coverage < 80';
    case 3:
      return 'coverage < 70';
    case 4:
      return 'coverage < 50';
    case 5:
      return 'coverage < 30';
    default:
      return '';
  }
};

const convertDuplications = duplications => {
  switch (duplications) {
    case 1:
      return 'duplicated_lines_density < 3';
    case 2:
      return 'duplicated_lines_density >= 3';
    case 3:
      return 'duplicated_lines_density >= 5';
    case 4:
      return 'duplicated_lines_density >= 10';
    case 5:
      return 'duplicated_lines_density >= 20';
    default:
      return '';
  }
};

const convertSize = size => {
  switch (size) {
    case 1:
      return 'ncloc < 1000';
    case 2:
      return 'ncloc >= 1000';
    case 3:
      return 'ncloc >= 10000';
    case 4:
      return 'ncloc >= 100000';
    case 5:
      return 'ncloc >= 500000';
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
    conditions.push('alert_status = ' + query['gate']);
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

  if (query['reliability'] != null) {
    conditions.push(convertIssuesRating('reliability_rating', query['reliability']));
  }

  if (query['security'] != null) {
    conditions.push(convertIssuesRating('security_rating', query['security']));
  }

  if (query['maintainability'] != null) {
    conditions.push(convertIssuesRating('sqale_rating', query['maintainability']));
  }

  return conditions.join(' and ');
};

export const mapMetricToProperty = metricKey => {
  const map = {
    'reliability_rating': 'reliability',
    'security_rating': 'security',
    'sqale_rating': 'maintainability',
    'coverage': 'coverage',
    'duplicated_lines_density': 'duplications',
    'ncloc': 'size',
    'alert_status': 'gate'
  };
  return map[metricKey];
};
