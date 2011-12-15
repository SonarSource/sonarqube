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
import org.sonar.api.web.dashboard.DashboardLayout;
import org.sonar.api.web.dashboard.DashboardTemplate;

/**
 * Hotspot dashboard for Sonar
 *
 * @since 2.13
 */
public final class HotspotsDashboard extends DashboardTemplate {

  private static final String HOTSPOT_METRIC = "hotspot_metric";
  private static final String TITLE_PROPERTY = "title";
  private static final String METRIC_PROPERTY = "metric";

  @Override
  public Dashboard createDashboard() {
    Dashboard dashboard = Dashboard.createByName("Hotspots");
    dashboard.setLayout(DashboardLayout.TWO_COLUMNS);

    addFirstColumn(dashboard);
    addSecondColumn(dashboard);

    return dashboard;
  }

  private void addFirstColumn(Dashboard dashboard) {
    dashboard.addWidget("hotspot_most_violated_rules", 1);

    Dashboard.Widget widget = dashboard.addWidget(HOTSPOT_METRIC, 1);
    widget.setProperty(METRIC_PROPERTY, "test_execution_time");
    widget.setProperty(TITLE_PROPERTY, "Longest unit tests");

    widget = dashboard.addWidget(HOTSPOT_METRIC, 1);
    widget.setProperty(METRIC_PROPERTY, "complexity");
    widget.setProperty(TITLE_PROPERTY, "Highest complexity");

    widget = dashboard.addWidget(HOTSPOT_METRIC, 1);
    widget.setProperty(METRIC_PROPERTY, "duplicated_lines");
    widget.setProperty(TITLE_PROPERTY, "Highest duplications");
  }

  private void addSecondColumn(Dashboard dashboard) {
    dashboard.addWidget("hotspot_most_violated_resources", 2);

    Dashboard.Widget widget = dashboard.addWidget(HOTSPOT_METRIC, 2);
    widget.setProperty(METRIC_PROPERTY, "uncovered_lines");
    widget.setProperty(TITLE_PROPERTY, "Highest untested lines");

    widget = dashboard.addWidget(HOTSPOT_METRIC, 2);
    widget.setProperty(METRIC_PROPERTY, "function_complexity");
    widget.setProperty(TITLE_PROPERTY, "Highest average method complexity");

    widget = dashboard.addWidget(HOTSPOT_METRIC, 2);
    widget.setProperty(METRIC_PROPERTY, "public_undocumented_api");
    widget.setProperty(TITLE_PROPERTY, "Most undocumented APIs");
  }

}