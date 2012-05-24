/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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

import org.sonar.plugins.core.widgets.FilterWidget;

import org.sonar.api.web.Dashboard.Widget;

import org.sonar.api.web.Dashboard;
import org.sonar.api.web.DashboardLayout;
import org.sonar.api.web.DashboardTemplate;

/**
 * Treemap global dashboard for Sonar
 *
 * @since 3.1
 */
public final class TreemapDashboard extends DashboardTemplate {
  @Override
  public String getName() {
    return "Treemap";
  }

  @Override
  public Dashboard createDashboard() {
    Dashboard dashboard = Dashboard.create();
    dashboard.setGlobal(true);
    dashboard.setLayout(DashboardLayout.ONE_COLUMN);

    Widget filterWidget = dashboard.addWidget("filter", 1);
    filterWidget.setProperty(FilterWidget.FILTER, "Treemap");

    return dashboard;
  }
}