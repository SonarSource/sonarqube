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
 * Default "Hotspots" dashboard
 *
 * @since 2.13
 */
public final class ProjectHotspotDashboard extends DashboardTemplate {

  private static final String HOTSPOT_METRIC = "hotspot_metric";
  private static final String METRIC_PROPERTY = "metric";

  @Override
  public String getName() {
    return "Hotspots";
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
    dashboard.addWidget("hotspot_most_violated_rules", 1);

    Dashboard.Widget widget = dashboard.addWidget(HOTSPOT_METRIC, 1);
    widget.setProperty(METRIC_PROPERTY, "test_execution_time");

    widget = dashboard.addWidget(HOTSPOT_METRIC, 1);
    widget.setProperty(METRIC_PROPERTY, "complexity");

    widget = dashboard.addWidget(HOTSPOT_METRIC, 1);
    widget.setProperty(METRIC_PROPERTY, "duplicated_lines");
  }

  private void addSecondColumn(Dashboard dashboard) {
    dashboard.addWidget("hotspot_most_violated_resources", 2);

    Dashboard.Widget widget = dashboard.addWidget(HOTSPOT_METRIC, 2);
    widget.setProperty(METRIC_PROPERTY, "uncovered_lines");

    widget = dashboard.addWidget(HOTSPOT_METRIC, 2);
    widget.setProperty(METRIC_PROPERTY, "function_complexity");

    widget = dashboard.addWidget(HOTSPOT_METRIC, 2);
    widget.setProperty(METRIC_PROPERTY, "public_undocumented_api");
  }

}