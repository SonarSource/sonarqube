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
import { ComponentQualifier } from '~sonar-aligned/types/component';
import { RawQuery } from '~sonar-aligned/types/router';
import { propertyToMetricMap, propertyToMetricMapLegacy } from './utils';

type Level = 'ERROR' | 'WARN' | 'OK';

export interface Query {
  [x: string]: string | number | string[] | undefined;
  coverage?: number;
  duplications?: number;
  gate?: Level;
  languages?: string[];
  maintainability?: number;
  new_coverage?: number;
  new_duplications?: number;
  new_lines?: number;
  new_maintainability?: number;
  new_reliability?: number;
  new_security?: number;
  new_security_review_rating?: number;
  qualifier?: ComponentQualifier;
  reliability?: number;
  search?: string;
  security?: number;
  security_review_rating?: number;
  size?: number;
  sort?: string;
  tags?: string[];
  view?: string;
}

export function parseUrlQuery(urlQuery: RawQuery): Query {
  return {
    gate: getAsLevel(urlQuery['gate']),
    reliability: getAsNumericRating(urlQuery['reliability']),
    new_reliability: getAsNumericRating(urlQuery['new_reliability']),
    security: getAsNumericRating(urlQuery['security']),
    new_security: getAsNumericRating(urlQuery['new_security']),
    security_review: getAsNumericRating(urlQuery['security_review']),
    new_security_review: getAsNumericRating(urlQuery['new_security_review']),
    maintainability: getAsNumericRating(urlQuery['maintainability']),
    new_maintainability: getAsNumericRating(urlQuery['new_maintainability']),
    coverage: getAsNumericRating(urlQuery['coverage']),
    new_coverage: getAsNumericRating(urlQuery['new_coverage']),
    duplications: getAsNumericRating(urlQuery['duplications']),
    new_duplications: getAsNumericRating(urlQuery['new_duplications']),
    size: getAsNumericRating(urlQuery['size']),
    new_lines: getAsNumericRating(urlQuery['new_lines']),
    languages: getAsStringArray(urlQuery['languages']),
    tags: getAsStringArray(urlQuery['tags']),
    qualifier: getAsQualifier(urlQuery['qualifier']),
    search: getAsString(urlQuery['search']),
    sort: getAsString(urlQuery['sort']),
    view: getView(urlQuery['view']),
  };
}

export function convertToFilter(query: Query, isFavorite: boolean, isLegacy: boolean): string {
  const conditions: string[] = [];

  if (isFavorite) {
    conditions.push('isFavorite');
  }

  if (query['gate'] != null) {
    conditions.push(mapPropertyToMetric('gate', isLegacy) + ' = ' + query['gate']);
  }

  ['coverage', 'new_coverage'].forEach((property) =>
    pushMetricToArray(query, property, conditions, convertCoverage, isLegacy),
  );

  ['duplications', 'new_duplications'].forEach((property) =>
    pushMetricToArray(query, property, conditions, convertDuplications, isLegacy),
  );

  ['size', 'new_lines'].forEach((property) =>
    pushMetricToArray(query, property, conditions, convertSize, isLegacy),
  );

  [
    'reliability',
    'security',
    'security_review',
    'maintainability',
    'new_reliability',
    'new_security',
    'new_security_review',
    'new_maintainability',
  ].forEach((property) =>
    pushMetricToArray(query, property, conditions, convertIssuesRating, isLegacy),
  );

  ['languages', 'tags', 'qualifier'].forEach((property) =>
    pushMetricToArray(query, property, conditions, convertArrayMetric, isLegacy),
  );

  if (query['search'] != null) {
    conditions.push(`${mapPropertyToMetric('search', isLegacy)} = "${query['search']}"`);
  }

  return conditions.join(' and ');
}

const viewParems = ['sort', 'view'];

export function hasFilterParams(query: Query) {
  return Object.keys(query)
    .filter((key) => !viewParems.includes(key))
    .some((key) => query[key] !== undefined);
}

export function hasViewParams(query: Query) {
  return Object.keys(query)
    .filter((key) => viewParems.includes(key))
    .some((key) => query[key] !== undefined);
}

function getAsNumericRating(value: any): number | undefined {
  if (value === '' || value == null || isNaN(value)) {
    return undefined;
  }
  const num = Number(value);
  return num > 0 && num < 7 ? num : undefined;
}

function getAsLevel(value: any): Level | undefined {
  if (value === 'ERROR' || value === 'WARN' || value === 'OK') {
    return value;
  }
  return undefined;
}

function getAsString(value: any): string | undefined {
  if (typeof value !== 'string' || value === '') {
    return undefined;
  }
  return value;
}

function getAsStringArray(value: any): string[] | undefined {
  if (typeof value !== 'string' || value === '') {
    return undefined;
  }
  return value.split(',');
}

function getAsQualifier(value: string | undefined): ComponentQualifier | undefined {
  return value ? (value as ComponentQualifier) : undefined;
}

function getView(value: any): string | undefined {
  return typeof value !== 'string' || value === 'overall' ? undefined : value;
}

function convertIssuesRating(metric: string, rating: number): string {
  if (rating > 1 && rating < 5) {
    return `${metric} >= ${rating}`;
  }

  return `${metric} = ${rating}`;
}

function convertCoverage(metric: string, coverage: number): string {
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
}

function convertDuplications(metric: string, duplications: number): string {
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
}

function convertSize(metric: string, size: number): string {
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
}

function mapPropertyToMetric(property?: string, isLegacy = false): string | undefined {
  return property && (isLegacy ? propertyToMetricMapLegacy : propertyToMetricMap)[property];
}

function pushMetricToArray(
  query: Query,
  property: string,
  conditionsArray: string[],
  convertFunction: (metric: string, value: Query[string]) => string,
  isLegacy: boolean,
): void {
  const metric = mapPropertyToMetric(property, isLegacy);
  if (query[property] !== undefined && metric !== undefined) {
    conditionsArray.push(convertFunction(metric, query[property]));
  }
}

function convertArrayMetric(metric: string, items: string | string[]): string {
  if (!Array.isArray(items) || items.length < 2) {
    return metric + ' = ' + items;
  }
  return `${metric} IN (${items.join(', ')})`;
}
