/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.startup;

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import org.picocontainer.Startable;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.api.web.Dashboard;
import org.sonar.api.web.DashboardTemplate;
import org.sonar.db.dashboard.ActiveDashboardDao;
import org.sonar.db.dashboard.ActiveDashboardDto;
import org.sonar.db.dashboard.DashboardDao;
import org.sonar.db.dashboard.DashboardDto;
import org.sonar.db.dashboard.WidgetDto;
import org.sonar.db.dashboard.WidgetPropertyDto;
import org.sonar.db.loadedtemplate.LoadedTemplateDao;
import org.sonar.db.loadedtemplate.LoadedTemplateDto;
import org.sonar.server.issue.filter.RegisterIssueFilters;

/**
 * @since 2.13
 */
public class RegisterDashboards implements Startable {
  private static final Logger LOG = Loggers.get(RegisterDashboards.class);

  static final String DEFAULT_DASHBOARD_NAME = "Dashboard";

  private final List<DashboardTemplate> dashboardTemplates;
  private final DashboardDao dashboardDao;
  private final ActiveDashboardDao activeDashboardDao;
  private final LoadedTemplateDao loadedTemplateDao;

  public RegisterDashboards(DashboardTemplate[] dashboardTemplatesArray, DashboardDao dashboardDao,
    ActiveDashboardDao activeDashboardDao, LoadedTemplateDao loadedTemplateDao, RegisterIssueFilters startupDependency) {
    this.dashboardTemplates = Lists.newArrayList(dashboardTemplatesArray);
    this.dashboardDao = dashboardDao;
    this.activeDashboardDao = activeDashboardDao;
    this.loadedTemplateDao = loadedTemplateDao;
    // RegisterIssueFilters must be run before this task, to be able to reference issue filters in widget properties
  }

  /**
   * Used when no plugin is defining some DashboardTemplate
   */
  public RegisterDashboards(DashboardDao dashboardDao, ActiveDashboardDao activeDashboardDao, LoadedTemplateDao loadedTemplateDao, RegisterIssueFilters registerIssueFilters) {
    this(new DashboardTemplate[] {}, dashboardDao, activeDashboardDao, loadedTemplateDao, registerIssueFilters);
  }

  @Override
  public void start() {
    Profiler profiler = Profiler.create(Loggers.get(getClass())).startInfo("Register dashboards");

    List<DashboardDto> registeredDashboards = Lists.newArrayList();
    for (DashboardTemplate template : dashboardTemplates) {
      if (shouldRegister(template.getName())) {
        Dashboard dashboard = template.createDashboard();
        DashboardDto dto = register(template.getName(), dashboard);
        if ((dto != null) && (dashboard.isActivated())) {
          registeredDashboards.add(dto);
        }
      }
    }

    activate(registeredDashboards);
    profiler.stopDebug();
  }

  @Override
  public void stop() {
    // nothing to do
  }

  protected void activate(List<DashboardDto> loadedDashboards) {
    int nextOrderIndex = activeDashboardDao.selectMaxOrderIndexForNullUser() + 1;

    for (DashboardDto dashboardDto : new DashboardOrdering().sortedCopy(loadedDashboards)) {
      activate(dashboardDto, nextOrderIndex);
      nextOrderIndex++;
    }
  }

  private void activate(DashboardDto dashboardDto, int index) {
    LOG.info("Register dashboard: " + dashboardDto.getName());
    ActiveDashboardDto activeDashboardDto = new ActiveDashboardDto()
      .setDashboardId(dashboardDto.getId())
      .setOrderIndex(index);
    activeDashboardDao.insert(activeDashboardDto);
  }

  protected DashboardDto register(String name, Dashboard dashboard) {
    DashboardDto dto = null;
    if (dashboardDao.selectGlobalDashboard(name) == null) {
      dto = createDtoFromExtension(name, dashboard);
      dashboardDao.insert(dto);
    }
    // and save the fact that is has now already been loaded
    loadedTemplateDao.insert(new LoadedTemplateDto(name, LoadedTemplateDto.DASHBOARD_TYPE));
    return dto;
  }

  protected DashboardDto createDtoFromExtension(String name, Dashboard dashboard) {
    Date now = new Date();

    DashboardDto dashboardDto = new DashboardDto()
      .setName(name)
      .setDescription(dashboard.getDescription())
      .setColumnLayout(dashboard.getLayout().getCode())
      .setShared(true)
      .setGlobal(dashboard.isGlobal());
    dashboardDto.setCreatedAt(now).setUpdatedAt(now);

    for (int columnIndex = 1; columnIndex <= dashboard.getLayout().getColumns(); columnIndex++) {
      List<Dashboard.Widget> widgets = dashboard.getWidgetsOfColumn(columnIndex);
      for (int rowIndex = 1; rowIndex <= widgets.size(); rowIndex++) {
        Dashboard.Widget widget = widgets.get(rowIndex - 1);
        WidgetDto widgetDto = new WidgetDto()
          .setWidgetKey(widget.getId())
          .setColumnIndex(columnIndex)
          .setRowIndex(rowIndex)
          .setConfigured(true);
        widgetDto.setCreatedAt(now).setUpdatedAt(now);
        dashboardDto.addWidget(widgetDto);

        for (Entry<String, String> property : widget.getProperties().entrySet()) {
          WidgetPropertyDto propDto = new WidgetPropertyDto()
            .setPropertyKey(property.getKey())
            .setTextValue(property.getValue());
          widgetDto.addWidgetProperty(propDto);
        }
      }
    }
    return dashboardDto;
  }

  protected boolean shouldRegister(String dashboardName) {
    return loadedTemplateDao.countByTypeAndKey(LoadedTemplateDto.DASHBOARD_TYPE, dashboardName) == 0;
  }

  private static class DashboardOrdering extends Ordering<DashboardDto> implements Serializable {
    private static final long serialVersionUID = 0;

    @Override
    public int compare(DashboardDto left, DashboardDto right) {
      String leftName = (left == null) ? null : left.getName();
      String rightName = (right == null) ? null : right.getName();

      // the default dashboard must be the first one to be activated
      if (DEFAULT_DASHBOARD_NAME.equals(leftName)) {
        return -1;
      }
      if (DEFAULT_DASHBOARD_NAME.equals(rightName)) {
        return 1;
      }

      return Ordering.natural().nullsLast().compare(leftName, rightName);
    }
  }
}
