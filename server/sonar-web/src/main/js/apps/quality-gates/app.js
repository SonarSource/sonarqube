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
import Gates from './gates';
import GatesView from './gates-view';
import ActionsView from './actions-view';
import Router from './router';
import Layout from './layout';
import Controller from './controller';

const App = new Marionette.Application();

const init = function () {
  let options = window.sonarqube;
  // Layout
  this.layout = new Layout({ el: options.el });
  this.layout.render();
  $('#footer').addClass('search-navigator-footer');

  // Gates List
  this.gates = new Gates();

  // Controller
  this.controller = new Controller({ app: this });

  // Header
  this.actionsView = new ActionsView({
    canEdit: this.canEdit,
    collection: this.gates
  });
  this.layout.actionsRegion.show(this.actionsView);

  // List
  this.gatesView = new GatesView({
    canEdit: this.canEdit,
    collection: this.gates
  });
  this.layout.resultsRegion.show(this.gatesView);

  // Router
  this.router = new Router({ app: this });
  Backbone.history.start({
    pushState: true,
    root: options.urlRoot
  });
};

const appXHR = $.get('/api/qualitygates/app')
    .done(function (r) {
      App.canEdit = r.edit;
      App.periods = r.periods;
      App.metrics = r.metrics;
    });

App.on('start', function (options) {
  appXHR.done(function () {
    init.call(App, options);
  });
});

window.sonarqube.appStarted.then(options => App.start(options));


