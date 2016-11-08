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
import State from './models/state';
import Layout from './layout';
import Issues from './models/issues';
import Facets from '../../components/navigator/models/facets';
import Controller from './controller';
import Router from './router';
import WorkspaceListView from './workspace-list-view';
import WorkspaceHeaderView from './workspace-header-view';
import FacetsView from './facets-view';

const App = new Marionette.Application();
const init = function () {
  const options = window.sonarqube;

  this.state = new State();
  this.list = new Issues();
  this.facets = new Facets();

  this.layout = new Layout({ app: this, el: options.el });
  this.layout.render();
  $('#footer').addClass('search-navigator-footer');

  this.controller = new Controller({ app: this });

  this.issuesView = new WorkspaceListView({
    app: this,
    collection: this.list
  });
  this.layout.workspaceListRegion.show(this.issuesView);
  this.issuesView.bindScrollEvents();

  this.workspaceHeaderView = new WorkspaceHeaderView({
    app: this,
    collection: this.list
  });
  this.layout.workspaceHeaderRegion.show(this.workspaceHeaderView);

  this.facetsView = new FacetsView({
    app: this,
    collection: this.facets
  });
  this.layout.facetsRegion.show(this.facetsView);

  key.setScope('list');
  App.router = new Router({ app: App });
  Backbone.history.start();
};

App.on('start', function () {
  init.call(App);
});

window.sonarqube.appStarted.then(options => App.start(options));

