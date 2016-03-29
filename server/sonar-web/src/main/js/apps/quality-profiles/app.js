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
import Profiles from './profiles';
import ActionsView from './actions-view';
import ProfilesView from './profiles-view';

const App = new Marionette.Application();
const requestUser = $.get(window.baseUrl + '/api/users/current').done(function (r) {
  App.canWrite = r.permissions.global.indexOf('profileadmin') !== -1;
});
const requestExporters = $.get(window.baseUrl + '/api/qualityprofiles/exporters').done(function (r) {
  App.exporters = r.exporters;
});
const init = function () {
  const options = window.sonarqube;

  // Layout
  this.layout = new Layout({ el: options.el });
  this.layout.render();
  $('#footer').addClass('search-navigator-footer');

  // Profiles List
  this.profiles = new Profiles();

  // Controller
  this.controller = new Controller({ app: this });

  // Actions View
  this.actionsView = new ActionsView({
    collection: this.profiles,
    canWrite: this.canWrite
  });
  this.actionsView.requestLanguages().done(function () {
    App.layout.actionsRegion.show(App.actionsView);
  });

  // Profiles View
  this.profilesView = new ProfilesView({
    collection: this.profiles,
    canWrite: this.canWrite
  });
  this.layout.resultsRegion.show(this.profilesView);

  // Router
  this.router = new Router({ app: this });
  Backbone.history.start({
    pushState: true,
    root: options.urlRoot
  });
};

App.on('start', function () {
  $.when(requestUser, requestExporters).done(function () {
    init.call(App);
  });
});

window.sonarqube.appStarted.then(options => App.start(options));


