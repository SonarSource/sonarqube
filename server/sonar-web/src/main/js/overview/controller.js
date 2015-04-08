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
  'overview/main/layout',
  'overview/main/gate-view',
  'overview/main/size-view',
  'overview/main/issues-view',
  'overview/main/debt-view',
  'overview/main/coverage-view',
  'overview/main/duplications-view'
], function (MainLayout,
             GateView,
             SizeView,
             IssuesView,
             DebtView,
             CoverageView,
             DuplicationsView) {

  return Marionette.Controller.extend({

    initialize: function (options) {
      this.state = options.state;
      this.layout = options.layout;
    },

    main: function () {
      var options = { model: this.state },
          mainLayout = new MainLayout(options);
      this.layout.mainRegion.show(mainLayout);
      mainLayout.gateRegion.show(new GateView(options));
      mainLayout.sizeRegion.show(new SizeView(options));
      mainLayout.issuesRegion.show(new IssuesView(options));
      mainLayout.debtRegion.show(new DebtView(options));
      mainLayout.coverageRegion.show(new CoverageView(options));
      mainLayout.duplicationsRegion.show(new DuplicationsView(options));
      this.state.fetch();
    }

  });

});
