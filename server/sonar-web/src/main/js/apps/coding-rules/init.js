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
import _ from 'underscore';
import Backbone from 'backbone';
import Marionette from 'backbone.marionette';
import State from './models/state';
import Layout from './layout';
import Rules from './models/rules';
import Facets from '../../components/navigator/models/facets';
import Controller from './controller';
import Router from '../../components/navigator/router';
import WorkspaceListView from './workspace-list-view';
import WorkspaceHeaderView from './workspace-header-view';
import FacetsView from './facets-view';
import FiltersView from './filters-view';

const App = new Marionette.Application();

App.on('start', function (el) {
  $.get(window.baseUrl + '/api/rules/app').done(function (r) {
    App.canWrite = r.canWrite;
    App.qualityProfiles = _.sortBy(r.qualityprofiles, ['name', 'lang']);
    App.languages = _.extend(r.languages, {
      none: 'None'
    });
    _.map(App.qualityProfiles, function (profile) {
      profile.language = App.languages[profile.lang];
    });
    App.repositories = r.repositories;
    App.statuses = r.statuses;
  }).done(() => {
    this.layout = new Layout({ el });
    this.layout.render();
    $('#footer').addClass('search-navigator-footer');

    this.state = new State();
    this.list = new Rules();
    this.facets = new Facets();

    this.controller = new Controller({ app: this });

    this.workspaceListView = new WorkspaceListView({
      app: this,
      collection: this.list
    });
    this.layout.workspaceListRegion.show(this.workspaceListView);
    this.workspaceListView.bindScrollEvents();

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

    this.filtersView = new FiltersView({
      app: this
    });
    this.layout.filtersRegion.show(this.filtersView);

    key.setScope('list');
    this.router = new Router({
      app: this
    });
    Backbone.history.start();
  });
});

export default function (el) {
  App.start(el);
}
