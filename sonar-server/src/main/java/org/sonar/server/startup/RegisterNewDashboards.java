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
package org.sonar.server.startup;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.api.web.Dashboard;
import org.sonar.api.web.DashboardTemplate;
import org.sonar.persistence.dashboard.*;
import org.sonar.persistence.template.LoadedTemplateDao;
import org.sonar.persistence.template.LoadedTemplateDto;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

/**
 * @since 2.13
 */
public final class RegisterNewDashboards {

  private static final Logger LOG = LoggerFactory.getLogger(RegisterNewDashboards.class);
  static final String DEFAULT_DASHBOARD_ID = "dashboard";

  private List<DashboardTemplate> dashboardTemplates;
  private DashboardDao dashboardDao;
  private ActiveDashboardDao activeDashboardDao;
  private LoadedTemplateDao loadedTemplateDao;

  public RegisterNewDashboards(DashboardDao dashboardDao, ActiveDashboardDao activeDashboardDao, LoadedTemplateDao loadedTemplateDao) {
    this(new DashboardTemplate[]{}, dashboardDao, activeDashboardDao, loadedTemplateDao);
  }

  public RegisterNewDashboards(DashboardTemplate[] dashboardTemplatesArray, DashboardDao dashboardDao,
                               ActiveDashboardDao activeDashboardDao, LoadedTemplateDao loadedTemplateDao) {
    this.dashboardTemplates = Lists.newArrayList(dashboardTemplatesArray);
    this.dashboardDao = dashboardDao;
    this.activeDashboardDao = activeDashboardDao;
    this.loadedTemplateDao = loadedTemplateDao;
  }

  public void start() {
    TimeProfiler profiler = new TimeProfiler().start("Register dashboards");

    // load the dashboards that need to be loaded
    List<DashboardDto> loadedDashboards = Lists.newArrayList();
    DashboardDto defaultDashboard = null;
    for (DashboardTemplate dashboardTemplate : dashboardTemplates) {
      Dashboard dashboard = dashboardTemplate.createDashboard();
      if (shouldRegister(dashboard)) {
        DashboardDto dashboardDto = registerDashboard(dashboard);
        if (DEFAULT_DASHBOARD_ID.equals(dashboard.getId())) {
          defaultDashboard = dashboardDto;
        } else {
          loadedDashboards.add(dashboardDto);
        }
      }
    }
    // and activate them
    activateDashboards(loadedDashboards, defaultDashboard);

    profiler.stop();
  }

  protected void activateDashboards(List<DashboardDto> loadedDashboards, DashboardDto defaultDashboard) {
    int nextOrderIndex;
    if (defaultDashboard != null) {
      activateDashboard(defaultDashboard, 1);
      nextOrderIndex = 2;
    } else {
      nextOrderIndex = activeDashboardDao.selectMaxOrderIndexForNullUser() + 1;
    }
    Collections.sort(loadedDashboards, new DashboardComparator());
    for (DashboardDto dashboardDto : loadedDashboards) {
      activateDashboard(dashboardDto, nextOrderIndex++);
    }
  }

  private void activateDashboard(DashboardDto dashboardDto, int index) {
    ActiveDashboardDto activeDashboardDto = new ActiveDashboardDto();
    activeDashboardDto.setDashboardId(dashboardDto.getId());
    activeDashboardDto.setOrderIndex(index);
    activeDashboardDao.insert(activeDashboardDto);
    LOG.info("New dashboard '" + dashboardDto.getName() + "' registered");
  }

  protected DashboardDto registerDashboard(Dashboard dashboard) {
    DashboardDto dto = createDtoFromExtension(dashboard);
    // save the new dashboard
    dashboardDao.insert(dto);
    // and save the fact that is has now already been loaded
    loadedTemplateDao.insert(new LoadedTemplateDto(dashboard.getId(), LoadedTemplateDto.DASHBOARD_TYPE));
    return dto;
  }

  protected DashboardDto createDtoFromExtension(Dashboard dashboard) {
    Date now = new Date();
    DashboardDto dashboardDto = new DashboardDto();
    dashboardDto.setKey(dashboard.getId());
    dashboardDto.setName(dashboard.getName());
    dashboardDto.setDescription(dashboard.getDescription());
    dashboardDto.setColumnLayout(dashboard.getLayout().getCode());
    dashboardDto.setShared(true);
    dashboardDto.setCreatedAt(now);
    dashboardDto.setUpdatedAt(now);

    for (int columnIndex = 1; columnIndex <= dashboard.getLayout().getColumns(); columnIndex++) {
      List<Dashboard.Widget> widgets = dashboard.getWidgetsOfColumn(columnIndex);
      for (int rowIndex = 1; rowIndex <= widgets.size(); rowIndex++) {
        Dashboard.Widget widget = widgets.get(rowIndex - 1);
        WidgetDto widgetDto = new WidgetDto();
        widgetDto.setKey(widget.getId());
        widgetDto.setColumnIndex(columnIndex);
        widgetDto.setRowIndex(rowIndex);
        widgetDto.setConfigured(true);
        widgetDto.setCreatedAt(now);
        widgetDto.setUpdatedAt(now);
        dashboardDto.addWidget(widgetDto);

        for (Entry<String, String> property : widget.getProperties().entrySet()) {
          WidgetPropertyDto propDto = new WidgetPropertyDto();
          propDto.setKey(property.getKey());
          propDto.setValue(property.getValue());
          widgetDto.addWidgetProperty(propDto);
        }
      }

    }
    return dashboardDto;
  }

  protected boolean shouldRegister(Dashboard dashboard) {
    return loadedTemplateDao.countByTypeAndKey(LoadedTemplateDto.DASHBOARD_TYPE, dashboard.getId()) == 0;
  }

  protected static class DashboardComparator implements Comparator<DashboardDto> {
    public int compare(DashboardDto d1, DashboardDto d2) {
      return d1.getName().compareTo(d2.getName());
    }
  }

}
