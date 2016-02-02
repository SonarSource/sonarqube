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
module.exports = function (componentKey, dashboardKey) {
  var params = [
    { key: 'id', value: componentKey },
    { key: 'did', value: dashboardKey }
  ];

  var matchPeriod = window.location.search.match(/period=(\d+)/);
  if (matchPeriod) {
    // If we have a match for period, check that it is not project-specific
    var period = parseInt(matchPeriod[1], 10);
    if (period <= 3) {
      params.push({ key: 'period', value: period });
    }
  }

  var query = params.map(function (p) {
    return p.key + '=' + encodeURIComponent(p.value);
  }).join('&');
  return '/dashboard/index?' + query;
};
