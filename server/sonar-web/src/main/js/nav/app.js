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
  'nav/global-navbar-view',
  'nav/context-navbar-view',
  'nav/settings-navbar-view',
  'workspace/main'
], function (GlobalNavbarView, ContextNavbarView, SettingsNavbarView) {

  var $ = jQuery,
      App = new Marionette.Application(),
      model = window.navbarOptions;

  App.addInitializer(function () {
    this.navbarView = new GlobalNavbarView({
      app: App,
      el: $('.navbar-global'),
      model: model
    });
    this.navbarView.render();
  });

  if (model.has('contextBreadcrumbs')) {
    App.addInitializer(function () {
      this.contextNavbarView = new ContextNavbarView({
        app: App,
        el: $('.navbar-context'),
        model: model
      });
      this.contextNavbarView.render();
    });
  }

  if (model.get('space') === 'settings') {
    App.addInitializer(function () {
      this.settingsNavbarView = new SettingsNavbarView({
        app: App,
        el: $('.navbar-context'),
        model: model
      });
      this.settingsNavbarView.render();
    });
  }

  App.addInitializer(function () {
    var that = this;
    $(window).on('keypress', function (e) {
      var tagName = e.target.tagName;
      if (tagName !== 'INPUT' && tagName !== 'SELECT' && tagName !== 'TEXTAREA') {
        var code = e.keyCode || e.which;
        if (code === 63) {
          that.navbarView.showShortcutsHelp();
        }
      }
    });
  });

  window.requestMessages().done(function () {
    App.start();
  });

});
