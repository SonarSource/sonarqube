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
  './router',
  './controller',
  './layout',
  './list',
  './list-view',
  './filters-view'
], function (Router, Controller, Layout, List, ListView, FiltersView) {

  var $ = jQuery,
      App = new Marionette.Application(),
      init = function (options) {

        // State
        this.state = new Backbone.Model({ internal: false });

        // Layout
        this.layout = new Layout({ el: options.el });
        this.layout.render();
        $('#footer').addClass('search-navigator-footer');

        // Web Services List
        this.list = new List();

        // Controller
        this.controller = new Controller({
          app: this,
          state: this.state
        });

        // List View
        this.listView = new ListView({
          collection: this.list,
          state: this.state
        });
        this.layout.resultsRegion.show(this.listView);

        // Filters View
        this.filtersView = new FiltersView({
          collection: this.list,
          state: this.state
        });
        this.layout.actionsRegion.show(this.filtersView);

        // Router
        this.router = new Router({ app: this });
        Backbone.history.start({
          pushState: true,
          root: getRoot()
        });
      };

  App.on('start', function (options) {
    window.requestMessages().done(function () {
      init.call(App, options);
    });
  });

  function getRoot () {
    var API_DOCUMENTATION = '/api_documentation',
        path = window.location.pathname,
        pos = path.indexOf(API_DOCUMENTATION);
    return path.substr(0, pos + API_DOCUMENTATION.length);
  }

  return App;

});
