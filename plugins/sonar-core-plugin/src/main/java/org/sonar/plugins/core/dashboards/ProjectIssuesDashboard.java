/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.plugins.core.dashboards;

import org.sonar.api.web.Dashboard;
import org.sonar.api.web.DashboardLayout;
import org.sonar.api.web.DashboardTemplate;

/**
 * Issues dashboard for Sonar
 *
 * @since 3.6
 */
public final class ProjectIssuesDashboard extends DashboardTemplate {

  @Override
  public String getName() {
    return "Issues";
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
    dashboard.addWidget("issues_action_plans", 1);
  }

  private void addSecondColumn(Dashboard dashboard) {
    dashboard.addWidget("unresolved_issues_per_assignee", 2);
    dashboard.addWidget("my_unresolved_issues", 2);
    dashboard.addWidget("false_positive_issues", 2);
  }

}