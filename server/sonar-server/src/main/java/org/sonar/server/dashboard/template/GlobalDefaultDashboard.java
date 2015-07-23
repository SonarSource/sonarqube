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
import org.sonar.db.measure.MeasureFilterDao;
import org.sonar.db.measure.MeasureFilterDto;
import org.sonar.server.dashboard.widget.MeasureFilterAsTreemapWidget;
import org.sonar.server.dashboard.widget.MeasureFilterListWidget;
import org.sonar.server.dashboard.widget.WelcomeWidget;
import org.sonar.server.measure.template.MyFavouritesFilter;
import org.sonar.server.measure.template.ProjectFilter;

/**
 * Projects global dashboard for Sonar
 *
 * @since 3.1
 */
public final class GlobalDefaultDashboard extends DashboardTemplate {

  private MeasureFilterDao filterDao;

  public GlobalDefaultDashboard(MeasureFilterDao filterDao) {
    this.filterDao = filterDao;
  }

  @Override
  public Dashboard createDashboard() {
    Dashboard dashboard = Dashboard.create()
      .setGlobal(true)
      .setLayout(DashboardLayout.TWO_COLUMNS);

    dashboard.addWidget(WelcomeWidget.ID, 1);
    addMyFavouritesWidget(dashboard);
    addProjectsWidgets(dashboard);

    return dashboard;
  }

  private void addMyFavouritesWidget(Dashboard dashboard) {
    MeasureFilterDto filter = findSystemFilter(MyFavouritesFilter.NAME);
    if (filter != null) {
      dashboard
        .addWidget(MeasureFilterListWidget.ID, 1)
        .setProperty(MeasureFilterListWidget.FILTER_PROPERTY, filter.getId().toString())
        .setProperty(MeasureFilterListWidget.PAGE_SIZE_PROPERTY, "50");
    }
  }

  private void addProjectsWidgets(Dashboard dashboard) {
    MeasureFilterDto filter = findSystemFilter(ProjectFilter.NAME);
    if (filter != null) {
      dashboard
        .addWidget(MeasureFilterListWidget.ID, 2)
        .setProperty(MeasureFilterListWidget.FILTER_PROPERTY, filter.getId().toString())
        .setProperty(MeasureFilterListWidget.PAGE_SIZE_PROPERTY, "20");

      dashboard
        .addWidget(MeasureFilterAsTreemapWidget.ID, 2)
        .setProperty(MeasureFilterListWidget.FILTER_PROPERTY, filter.getId().toString())
        .setProperty(MeasureFilterAsTreemapWidget.SIZE_METRIC_PROPERTY, "ncloc")
        .setProperty(MeasureFilterAsTreemapWidget.COLOR_METRIC_PROPERTY, "coverage");
    }
  }

  @Override
  public String getName() {
    return "Home";
  }

  private MeasureFilterDto findSystemFilter(String name) {
    return filterDao.selectSystemFilterByName(name);
  }
}
