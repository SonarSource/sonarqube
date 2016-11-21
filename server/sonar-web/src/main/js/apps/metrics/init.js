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
import Marionette from 'backbone.marionette';
import Layout from './layout';
import Metrics from './metrics';
import HeaderView from './header-view';
import ListView from './list-view';
import ListFooterView from './list-footer-view';

const App = new Marionette.Application();
const init = function (el) {
  // Layout
  this.layout = new Layout({ el });
  this.layout.render();

  // Collection
  this.metrics = new Metrics();

  // Header View
  this.headerView = new HeaderView({
    collection: this.metrics,
    domains: this.domains,
    types: this.types,
    app: App
  });
  this.layout.headerRegion.show(this.headerView);

  // List View
  this.listView = new ListView({
    collection: this.metrics,
    domains: this.domains,
    types: this.types
  });
  this.layout.listRegion.show(this.listView);

  // List Footer View
  this.listFooterView = new ListFooterView({ collection: this.metrics });
  this.layout.listFooterRegion.show(this.listFooterView);

  // Go!
  this.metrics.fetch();
};

App.requestDomains = function () {
  return $.get(window.baseUrl + '/api/metrics/domains').done(function (r) {
    App.domains = r.domains;
  });
};
App.requestTypes = function () {
  return $.get(window.baseUrl + '/api/metrics/types').done(function (r) {
    App.types = r.types;
  });
};

App.on('start', function (el) {
  $.when(App.requestDomains(), App.requestTypes()).done(function () {
    init.call(App, el);
  });
});

export default function (el) {
  App.start(el);
}

