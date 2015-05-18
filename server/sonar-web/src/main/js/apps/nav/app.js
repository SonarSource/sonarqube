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
  './state',
  './global-navbar-view',
  './context-navbar-view',
  './settings-navbar-view',
  'components/workspace/main'
], function (State, GlobalNavbarView, ContextNavbarView, SettingsNavbarView) {

  var $ = jQuery,
      App = new Marionette.Application(),
      state = new State();

  state.set(window.navbarOptions.toJSON());

  App.on('start', function () {
    state.fetchGlobal();

    this.navbarView = new GlobalNavbarView({
      app: App,
      el: $('.navbar-global'),
      model: state
    });
    this.navbarView.render();

    if (state.get('space') === 'component') {
      state.fetchComponent();
      this.contextNavbarView = new ContextNavbarView({
        app: App,
        el: $('.navbar-context'),
        model: state
      });
      this.contextNavbarView.render();
    }

    if (state.get('space') === 'settings') {
      state.fetchSettings();
      this.settingsNavbarView = new SettingsNavbarView({
        app: App,
        el: $('.navbar-context'),
        model: state
      });
      this.settingsNavbarView.render();
    }

    $(window).on('keypress', function (e) {
      var tagName = e.target.tagName;
      if (tagName !== 'INPUT' && tagName !== 'SELECT' && tagName !== 'TEXTAREA') {
        var code = e.keyCode || e.which;
        if (code === 63) {
          App.navbarView.showShortcutsHelp();
        }
      }
    });
  });

  window.requestMessages().done(function () {
    App.start();
  });

});
