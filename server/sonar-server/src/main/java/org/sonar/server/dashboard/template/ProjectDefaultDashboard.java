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
package org.sonar.server.dashboard.template;

import org.sonar.api.web.Dashboard;
import org.sonar.api.web.DashboardLayout;
import org.sonar.api.web.DashboardTemplate;

/**
 * Default dashboard
 *
 * @since 2.13
 */
public final class ProjectDefaultDashboard extends DashboardTemplate {

  @Override
  public String getName() {
    return "Dashboard";
  }

  @Override
  public Dashboard createDashboard() {
    Dashboard dashboard = Dashboard.create();
    dashboard.setLayout(DashboardLayout.TWO_COLUMNS);
    addFirstColumn(dashboard);
    addSecondColumn(dashboard);
    return dashboard;
  }

  private void addFirstColumn(Dashboard dashboard) {
    dashboard.addWidget("size", 1);
    dashboard.addWidget("duplications", 1);
    dashboard.addWidget("complexity", 1);
    dashboard.addWidget("events", 1);
    dashboard.addWidget("description", 1);
  }

  private void addSecondColumn(Dashboard dashboard) {
    dashboard.addWidget("debt_overview", 2);
    dashboard.addWidget("rules", 2);
    dashboard.addWidget("alerts", 2);
    dashboard.addWidget("code_coverage", 2);
  }

}
