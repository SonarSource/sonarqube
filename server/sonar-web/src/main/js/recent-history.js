/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
window.Sonar = {};

window.Sonar.RecentHistory = function () {
};

window.Sonar.RecentHistory.prototype.getRecentHistory = function () {
  var sonarHistory = localStorage.getItem('sonar_recent_history');
  if (sonarHistory == null) {
    sonarHistory = [];
  } else {
    sonarHistory = JSON.parse(sonarHistory);
  }
  return sonarHistory;
};

window.Sonar.RecentHistory.prototype.clear = function () {
  localStorage.removeItem('sonar_recent_history');
};

window.Sonar.RecentHistory.prototype.add = function (resourceKey, resourceName, icon) {
  var sonarHistory = this.getRecentHistory();

  if (resourceKey !== '') {
    var newEntry = {'key': resourceKey, 'name': resourceName, 'icon': icon};
    // removes the element of the array if it exists
    for (var i = 0; i < sonarHistory.length; i++) {
      var item = sonarHistory[i];
      if (item.key === resourceKey) {
        sonarHistory.splice(i, 1);
        break;
      }
    }
    // then add it to the beginning of the array
    sonarHistory.unshift(newEntry);
    // and finally slice the array to keep only 10 elements
    sonarHistory = sonarHistory.slice(0, 10);

    localStorage.setItem('sonar_recent_history', JSON.stringify(sonarHistory));
  }
};

window.Sonar.RecentHistory.prototype.populateRecentHistoryPanel = function () {
  var historyLinksList = $j('#recent-history-list');
  historyLinksList.empty();

  var recentHistory = this.getRecentHistory();
  if (recentHistory.length === 0) {
    $j('#recent-history').hide();
  } else {
    recentHistory.forEach(function (resource) {
      historyLinksList.append('<li><i class="icon-qualifier-' + resource.icon + '"></i><a href="' +
          baseUrl + '/dashboard/index/' + resource.key + dashboardParameters() + '"> ' + resource.name + '</a></li>');
    });
    $j('#recent-history').show();
  }
};
