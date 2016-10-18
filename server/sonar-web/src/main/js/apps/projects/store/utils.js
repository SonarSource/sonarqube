/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
const getAsNumber = value => {
  if (value === '' || value == null) {
    return null;
  }
  return isNaN(value) ? null : Number(value);
};

const getAsLevel = value => {
  if (value === 'ERROR' || value === 'WARN' || value === 'OK') {
    return value;
  }
  return null;
};

export const parseUrlQuery = urlQuery => ({
  'gate': getAsLevel(urlQuery['gate']),

  'coverage__gte': getAsNumber(urlQuery['coverage__gte']),
  'coverage__lt': getAsNumber(urlQuery['coverage__lt']),

  'duplications__gte': getAsNumber(urlQuery['duplications__gte']),
  'duplications__lt': getAsNumber(urlQuery['duplications__lt']),

  'size__gte': getAsNumber(urlQuery['size__gte']),
  'size__lt': getAsNumber(urlQuery['size__lt'])
});

export const convertToFilter = query => {
  const conditions = [];

  if (query['gate'] != null) {
    conditions.push('alert_status = ' + query['gate']);
  }

  if (query['coverage__gte'] != null) {
    conditions.push('coverage >= ' + query['coverage__gte']);
  }

  if (query['coverage__lt'] != null) {
    conditions.push('coverage < ' + query['coverage__lt']);
  }

  if (query['duplications__gte'] != null) {
    conditions.push('duplicated_lines_density >= ' + query['duplications__gte']);
  }

  if (query['duplications__lt'] != null) {
    conditions.push('duplicated_lines_density < ' + query['duplications__lt']);
  }

  if (query['size__gte'] != null) {
    conditions.push('ncloc >= ' + query['size__gte']);
  }

  if (query['size__lt'] != null) {
    conditions.push('ncloc < ' + query['size__lt']);
  }

  return conditions.join(' and ');
};
