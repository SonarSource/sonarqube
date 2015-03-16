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
  'templates/nav'
], function () {

  var $ = jQuery;

  return Marionette.ItemView.extend({
    template: Templates['nav-context-navbar'],

    modelEvents: {
      'change': 'render'
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
          isOverviewActive = href.indexOf('/dashboard/') !== -1 && search.indexOf('did=') === -1,
          isMoreActive = !isOverviewActive && href.indexOf('/components') === -1 &&
              href.indexOf('/component_issues') === -1;

      return _.extend(Marionette.Layout.prototype.serializeData.apply(this, arguments), {
        canManageContextDashboards: window.SS.user != null,
        contextKeyEncoded: encodeURIComponent(this.model.get('contextKey')),

        isOverviewActive: isOverviewActive,
        isMoreActive: isMoreActive
      });
    }
  });

});
