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
import org.sonar.api.web.dashboard.Widget;

/**
 * Hotspot dashboard for Sonar
 */
public class HotspotsDashboard extends DashboardTemplate {

  private static final String HOTSPOT_METRIC = "hotspot_metric";
  private static final String TITLE_PROPERTY = "title";
  private static final String METRIC_PROPERTY = "metric";

  @Override
  public org.sonar.api.web.dashboard.Dashboard createDashboard() {
    Dashboard dashboard = Dashboard.createDashboard("sonar-hotspots", "Hotspots", DashboardLayouts.TWO_COLUMNS);

    Widget widget = dashboard.addWidget("hotspot_most_violated_rules", 1, 1);

    widget = dashboard.addWidget(HOTSPOT_METRIC, 1, 2);
    widget.addProperty(METRIC_PROPERTY, "test_execution_time");
    widget.addProperty(TITLE_PROPERTY, "Longest unit tests");

    widget = dashboard.addWidget(HOTSPOT_METRIC, 1, 3);
    widget.addProperty(METRIC_PROPERTY, "complexity");
    widget.addProperty(TITLE_PROPERTY, "Highest complexity");

    widget = dashboard.addWidget(HOTSPOT_METRIC, 1, 4);
    widget.addProperty(METRIC_PROPERTY, "duplicated_lines");
    widget.addProperty(TITLE_PROPERTY, "Highest duplications");

    widget = dashboard.addWidget("hotspot_most_violated_resources", 2, 1);

    widget = dashboard.addWidget(HOTSPOT_METRIC, 2, 2);
    widget.addProperty(METRIC_PROPERTY, "uncovered_lines");
    widget.addProperty(TITLE_PROPERTY, "Highest untested lines");

    widget = dashboard.addWidget(HOTSPOT_METRIC, 2, 3);
    widget.addProperty(METRIC_PROPERTY, "function_complexity");
    widget.addProperty(TITLE_PROPERTY, "Highest average method complexity");

    widget = dashboard.addWidget(HOTSPOT_METRIC, 2, 4);
    widget.addProperty(METRIC_PROPERTY, "public_undocumented_api");
    widget.addProperty(TITLE_PROPERTY, "Most undocumented APIs");

    return dashboard;
  }

}