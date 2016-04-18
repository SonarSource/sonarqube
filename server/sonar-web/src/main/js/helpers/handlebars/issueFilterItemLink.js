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
import _ from 'underscore';

function getQuery (query, separator) {
  separator = separator || '|';
  const route = [];
  _.forEach(query, function (value, property) {
    route.push(`${property}=${encodeURIComponent(value)}`);
  });
  return route.join(separator);
}

module.exports = function (query, property, value, mode) {
  const criterion = {};
  criterion[property] = value;
  const r = _.extend({}, query, criterion);
  if (mode === 'effort') {
    r.facetMode = 'effort';
  }
  if (r.componentKey != null) {
    return window.baseUrl + '/component_issues/index?id=' + encodeURIComponent(r.componentKey) +
        '#' + getQuery(_.omit(r, 'componentKey'));
  } else {
    return window.baseUrl + '/issues/search#' + getQuery(r);
  }
};
