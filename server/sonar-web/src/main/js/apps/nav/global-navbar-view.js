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
  './search-view',
  './shortcuts-help-view',
  './templates'
], function (SearchView, ShortcutsHelpView) {

  return Marionette.Layout.extend({
    template: Templates['nav-global-navbar'],

    modelEvents: {
      'change': 'render'
    },

    regions: {
      searchRegion: '.js-search-region'
    },

    events: {
      'click .js-login': 'onLoginClick',
      'click .js-favorite': 'onFavoriteClick',
      'show.bs.dropdown .js-search-dropdown': 'onSearchDropdownShow',
      'hidden.bs.dropdown .js-search-dropdown': 'onSearchDropdownHidden',
      'click .js-shortcuts': 'onShortcutsClick'
    },

    onRender: function () {
      var that = this;
      if (this.model.has('space')) {
        this.$el.addClass('navbar-' + this.model.get('space'));
      }
      this.$el.addClass('navbar-fade');
      setTimeout(function () {
        that.$el.addClass('in');
      }, 0);
    },

    onLoginClick: function () {
      var returnTo = window.location.pathname + window.location.search;
      window.location = baseUrl + '/sessions/new?return_to=' + encodeURIComponent(returnTo) + window.location.hash;
      return false;
    },

    onSearchDropdownShow: function () {
      var that = this;
      this.searchRegion.show(new SearchView({
        model: this.model,
        hide: function () {
          that.$('.js-search-dropdown-toggle').dropdown('toggle');
        }
      }));
    },

    onSearchDropdownHidden: function () {
      this.searchRegion.reset();
    },

    onShortcutsClick: function () {
      this.showShortcutsHelp();
    },

    showShortcutsHelp: function () {
      new ShortcutsHelpView({ shortcuts: this.model.get('shortcuts') }).render();
    },

    serializeData: function () {
      return _.extend(Marionette.Layout.prototype.serializeData.apply(this, arguments), {
        user: window.SS.user,
        userName: window.SS.userName,
        userEmail: window.SS.userEmail,
        isUserAdmin: window.SS.isUserAdmin,

        canManageGlobalDashboards: !!window.SS.user,
        canManageIssueFilters: !!window.SS.user,
        canManageMeasureFilters: !!window.SS.user
      });
    }
  });

});
