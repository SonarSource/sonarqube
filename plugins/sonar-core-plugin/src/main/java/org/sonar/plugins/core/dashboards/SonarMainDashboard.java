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

@DashboardWidgets ({
  @DashboardWidget(id="size", columnIndex=1, rowIndex=1),
  @DashboardWidget(id="comments_duplications", columnIndex=1, rowIndex=2),
  @DashboardWidget(id="complexity", columnIndex=1, rowIndex=3),
  @DashboardWidget(id="code_coverage", columnIndex=1, rowIndex=4),
  @DashboardWidget(id="events", columnIndex=1, rowIndex=5),
  @DashboardWidget(id="description", columnIndex=1, rowIndex=6),
  @DashboardWidget(id="rules", columnIndex=2, rowIndex=1),
  @DashboardWidget(id="alerts", columnIndex=2, rowIndex=2),
  @DashboardWidget(id="file_design", columnIndex=2, rowIndex=3),
  @DashboardWidget(id="package_design", columnIndex=2, rowIndex=4),
  @DashboardWidget(id="ckjm", columnIndex=2, rowIndex=5)
})
/**
 * Default dashboard for Sonar
 */
public class SonarMainDashboard extends AbstractDashboard implements Dashboard {

  @Override
  public String getId() {
    return "sonar-main-dashboard";
  }

  @Override
  public String getName() {
    return "Dashboard";
  }
  
  @Override
  public String getLayout() {
    return DashboardLayouts.TWO_COLUMNS;
  }

}