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
define([
  './templates'
], function () {

  var $ = jQuery,
      MORE_URLS = [
          '/dashboards', '/plugins/resource'
      ],
      SETTINGS_URLS = [
        '/project/settings', '/project/profile', '/project/qualitygate', '/manual_measures/index',
        '/action_plans/index', '/project/links', '/project_roles/index', '/project/history', '/project/key',
        '/project/deletion'
      ];

  return Marionette.ItemView.extend({
    template: Templates['nav-context-navbar'],

    modelEvents: {
      'change:component': 'render'
    },

    events: {
      'click .js-favorite': 'onFavoriteClick'
    },

    onRender: function () {
      this.$('[data-toggle="tooltip"]').tooltip({
        container: 'body'
      });
    },

    onFavoriteClick: function () {
      var that = this,
          url = baseUrl + '/favourites/toggle/' + this.model.get('contextId'),
          isContextFavorite = this.model.get('isContextFavorite');
      this.model.set({ isContextFavorite: !isContextFavorite });
      return $.post(url).fail(function () {
        that.model.set({ isContextFavorite: isContextFavorite });
      });
    },

    serializeData: function () {
      var href = window.location.href,
          search = window.location.search,
          isMoreActive = _.some(MORE_URLS, function (url) {
            return href.indexOf(url) !== -1;
          }) || (href.indexOf('/dashboard') !== -1 && search.indexOf('did=') !== -1),
          isSettingsActive = _.some(SETTINGS_URLS, function (url) {
            return href.indexOf(url) !== -1;
          }),
          isOverviewActive = !isMoreActive && href.indexOf('/dashboard') !== -1 && search.indexOf('did=') === -1;
      return _.extend(Marionette.Layout.prototype.serializeData.apply(this, arguments), {
        canManageContextDashboards: !!window.SS.user,
        contextKeyEncoded: encodeURIComponent(this.model.get('componentKey')),

        isOverviewActive: isOverviewActive,
        isSettingsActive: isSettingsActive,
        isMoreActive: isMoreActive
      });
    }
  });

});
