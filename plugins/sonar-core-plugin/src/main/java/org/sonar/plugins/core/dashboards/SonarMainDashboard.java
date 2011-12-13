/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.core.dashboards;

import org.sonar.api.web.dashboard.Dashboard;
import org.sonar.api.web.dashboard.DashboardLayouts;
import org.sonar.api.web.dashboard.DashboardTemplate;

/**
 * Default dashboard for Sonar
 */
public class SonarMainDashboard extends DashboardTemplate {

  @Override
  public org.sonar.api.web.dashboard.Dashboard createDashboard() {
    Dashboard dashboard = Dashboard.createDashboard("sonar-main", "Dashboard", DashboardLayouts.TWO_COLUMNS);
    dashboard.addWidget("size", 1, 1);
    dashboard.addWidget("comments_duplications", 1, 2);
    dashboard.addWidget("complexity", 1, 3);
    dashboard.addWidget("code_coverage", 1, 4);
    dashboard.addWidget("events", 1, 5);
    dashboard.addWidget("description", 1, 6);
    dashboard.addWidget("rules", 2, 1);
    dashboard.addWidget("alerts", 2, 2);
    dashboard.addWidget("file_design", 2, 3);
    dashboard.addWidget("package_design", 2, 4);
    dashboard.addWidget("ckjm", 2, 5);
    return dashboard;
  }

}