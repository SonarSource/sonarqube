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
import Backbone from 'backbone';
import Marionette from 'backbone.marionette';
import Layout from './layout';
import HeaderView from './header-view';
import SearchView from './search-view';
import ListView from './list-view';
import FooterView from './footer-view';
import Controller from './controller';
import Router from './router';
import Plugins from './plugins';

const App = new Marionette.Application();
const init = function (el) {
  // State
  this.state = new Backbone.Model({
    updateCenterActive: window.SS.updateCenterActive
  });

  // Layout
  this.layout = new Layout({ el });
  this.layout.render();

  // Plugins
  this.plugins = new Plugins();

  // Controller
  this.controller = new Controller({ collection: this.plugins, state: this.state });

  // Router
  this.router = new Router({ controller: this.controller });

  // Header
  this.headerView = new HeaderView({ collection: this.plugins });
  this.layout.headerRegion.show(this.headerView);

  // Search
  this.searchView = new SearchView({ collection: this.plugins, router: this.router, state: this.state });
  this.layout.searchRegion.show(this.searchView);
  this.searchView.focusSearch();

  // List
  this.listView = new ListView({ collection: this.plugins });
  this.layout.listRegion.show(this.listView);

  // Footer
  this.footerView = new FooterView({ collection: this.plugins });
  this.layout.footerRegion.show(this.footerView);

  // Go
  Backbone.history.start({
    pushState: true,
    root: window.baseUrl + '/updatecenter'
  });
};

App.on('start', function (el) {
  init.call(App, el);
});

export default function (el) {
  App.start(el);
}
