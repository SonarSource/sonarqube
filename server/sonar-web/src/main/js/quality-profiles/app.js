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
require([
  'quality-profiles/router',
  'quality-profiles/controller',
  'quality-profiles/layout',
  'quality-profiles/profiles',
  'quality-profiles/actions-view',
  'quality-profiles/profiles-view'
], function (Router, Controller, Layout, Profiles, ActionsView, ProfilesView) {

  var $ = jQuery,
      App = new Marionette.Application();

  App.on('start', function () {
    // Layout
    this.layout = new Layout({ el: '#quality-profiles' });
    this.layout.render();
    $('#footer').addClass('search-navigator-footer');

    // Profiles List
    this.profiles = new Profiles();

    // Controller
    this.controller = new Controller({ app: this });

    // Actions View
    this.actionsView = new ActionsView({
      collection: this.profiles
    });
    this.layout.actionsRegion.show(this.actionsView);

    // Profiles View
    this.profilesView = new ProfilesView({
      collection: this.profiles
    });
    this.layout.resultsRegion.show(this.profilesView);

    // Router
    this.router = new Router({ app: this });
    Backbone.history.start({
      pushState: true,
      root: getRoot()
    });

  });

  window.requestMessages().done(function () {
    App.start();
  });

  function getRoot () {
    var QUALITY_PROFILES = '/profiles',
        path = window.location.pathname,
        pos = path.indexOf(QUALITY_PROFILES);
    return path.substr(0, pos + QUALITY_PROFILES.length);
  }

});
