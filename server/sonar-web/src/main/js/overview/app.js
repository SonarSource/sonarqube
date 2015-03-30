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
requirejs([
  'overview/layout',
  'overview/models/state',
  'overview/views/gate-view',
  'overview/views/size-view',
  'overview/views/issues-view',
  'overview/views/debt-view',
  'overview/views/coverage-view',
  'overview/views/duplications-view'
], function (Layout,
             State,
             GateView,
             SizeView,
             IssuesView,
             DebtView,
             CoverageView,
             DuplicationsView) {

  var $ = jQuery,
      App = new Marionette.Application();

  App.addInitializer(function () {
    $('body').addClass('dashboard-page');
    this.state = new State(window.overviewConf);
    this.layout = new Layout({
      el: '.overview',
      model: this.state
    }).render();
    this.layout.gateRegion.show(new GateView({ model: this.state }));
    this.layout.sizeRegion.show(new SizeView({ model: this.state }));
    this.layout.issuesRegion.show(new IssuesView({ model: this.state }));
    this.layout.debtRegion.show(new DebtView({ model: this.state }));
    this.layout.coverageRegion.show(new CoverageView({ model: this.state }));
    this.layout.duplicationsRegion.show(new DuplicationsView({ model: this.state }));
    this.state.fetch();
  });

  window.requestMessages().done(function () {
    App.start();
  });

});
