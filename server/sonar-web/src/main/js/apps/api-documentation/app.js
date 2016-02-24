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
import $ from 'jquery';
import Backbone from 'backbone';
import Marionette from 'backbone.marionette';
import Router from './router';
import Controller from './controller';
import Layout from './layout';
import List from './list';
import ListView from './list-view';
import FiltersView from './filters-view';
import SearchView from './search-view';

const App = new Marionette.Application();
const init = function () {
  const options = window.sonarqube;

  // State
  this.state = new Backbone.Model({ internal: false });
  this.state.match = function (test, internal) {
    const pattern = new RegExp(this.get('query'), 'i');
    const internalCheck = !this.get('internal') && internal;
    return test.search(pattern) !== -1 && !internalCheck;
  };

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

  // Search View
  this.searchView = new SearchView({
    state: this.state
  });
  this.layout.searchRegion.show(this.searchView);

  // Router
  this.router = new Router({ app: this });
  Backbone.history.start({
    pushState: true,
    root: options.urlRoot
  });
};

App.on('start', function (options) {
  init.call(App, options);
});

window.sonarqube.appStarted.then(options => App.start(options));
