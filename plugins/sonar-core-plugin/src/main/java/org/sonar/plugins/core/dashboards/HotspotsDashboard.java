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

import org.sonar.api.web.AbstractDashboard;
import org.sonar.api.web.Dashboard;
import org.sonar.api.web.DashboardLayouts;
import org.sonar.api.web.DashboardWidget;
import org.sonar.api.web.DashboardWidgets;
import org.sonar.api.web.WidgetProperty;
import org.sonar.api.web.WidgetPropertyType;

@DashboardWidgets ({
  @DashboardWidget(id="hotspot_most_violated_rules", columnIndex=1, rowIndex=1),
  @DashboardWidget(id="hotspot_metric", columnIndex=1, rowIndex=2,
                    properties={
                      @WidgetProperty(key = "metric", type = WidgetPropertyType.METRIC, defaultValue = "test_execution_time"),
                      @WidgetProperty(key = "title", type = WidgetPropertyType.STRING, defaultValue = "Longest unit tests")

  }),
  @DashboardWidget(id="hotspot_metric", columnIndex=1, rowIndex=3,
                    properties={
                      @WidgetProperty(key = "metric", type = WidgetPropertyType.METRIC, defaultValue = "complexity"),
                      @WidgetProperty(key = "title", type = WidgetPropertyType.STRING, defaultValue = "Highest complexity")

  }),
  @DashboardWidget(id="hotspot_metric", columnIndex=1, rowIndex=4,
                    properties={
                      @WidgetProperty(key = "metric", type = WidgetPropertyType.METRIC, defaultValue = "duplicated_lines"),
                      @WidgetProperty(key = "title", type = WidgetPropertyType.STRING, defaultValue = "Highest duplications")

  }),
  @DashboardWidget(id="hotspot_most_violated_resources", columnIndex=2, rowIndex=1),
  @DashboardWidget(id="hotspot_metric", columnIndex=2, rowIndex=2,
                    properties={
                      @WidgetProperty(key = "metric", type = WidgetPropertyType.METRIC, defaultValue = "uncovered_lines"),
                      @WidgetProperty(key = "title", type = WidgetPropertyType.STRING, defaultValue = "Highest untested lines")

  }),
  @DashboardWidget(id="hotspot_metric", columnIndex=2, rowIndex=3,
                    properties={
                      @WidgetProperty(key = "metric", type = WidgetPropertyType.METRIC, defaultValue = "function_complexity"),
                      @WidgetProperty(key = "title", type = WidgetPropertyType.STRING, defaultValue = "Highest average method complexity")

  }),
  @DashboardWidget(id="hotspot_metric", columnIndex=2, rowIndex=4,
                    properties={
                      @WidgetProperty(key = "metric", type = WidgetPropertyType.METRIC, defaultValue = "public_undocumented_api"),
                      @WidgetProperty(key = "title", type = WidgetPropertyType.STRING, defaultValue = "Most undocumented APIs")

  })
})
/**
 * Hotspot dashboard for Sonar
 */
public class HotspotsDashboard extends AbstractDashboard implements Dashboard {

  @Override
  public String getId() {
    return "sonar-hotspots-dashboard";
  }

  @Override
  public String getName() {
    return "Hotspots";
  }
  
  @Override
  public String getLayout() {
    return DashboardLayouts.TWO_COLUMNS;
  }

}