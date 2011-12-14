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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.api.web.dashboard.Dashboard;
import org.sonar.api.web.dashboard.DashboardTemplate;
import org.sonar.api.web.dashboard.Widget;
import org.sonar.core.i18n.I18nManager;
import org.sonar.persistence.dashboard.ActiveDashboardDao;
import org.sonar.persistence.dashboard.ActiveDashboardDto;
import org.sonar.persistence.dashboard.DashboardDao;
import org.sonar.persistence.template.LoadedTemplateDao;
import org.sonar.persistence.template.LoadedTemplateDto;

import com.google.common.collect.Lists;

public final class RegisterProvidedDashboards {

  private static final Logger LOGGER = LoggerFactory.getLogger(RegisterProvidedDashboards.class);
  private static final String MAIN_DASHBOARD_ID = "sonar-main";

  private List<DashboardTemplate> dashboardTemplates;
  private DashboardDao dashboardDao;
  private ActiveDashboardDao activeDashboardDao;
  private LoadedTemplateDao loadedTemplateDao;
  private I18nManager i18nManager;

  public RegisterProvidedDashboards(DashboardDao dashboardDao, ActiveDashboardDao activeDashboardDao, LoadedTemplateDao loadedTemplateDao,
      I18nManager i18nManager) {
    this(new DashboardTemplate[] {}, dashboardDao, activeDashboardDao, loadedTemplateDao, i18nManager);
  }

  public RegisterProvidedDashboards(DashboardTemplate[] dashboardTemplatesArray, DashboardDao dashboardDao,
      ActiveDashboardDao activeDashboardDao, LoadedTemplateDao loadedTemplateDao, I18nManager i18nManager) {
    this.dashboardTemplates = Lists.newArrayList(dashboardTemplatesArray);
    this.dashboardDao = dashboardDao;
    this.activeDashboardDao = activeDashboardDao;
    this.loadedTemplateDao = loadedTemplateDao;
    this.i18nManager = i18nManager;
  }

  public void start() {
    TimeProfiler profiler = new TimeProfiler().start("Load provided dashboards");

    // load the dashboards that need to be loaded
    ArrayList<org.sonar.persistence.dashboard.DashboardDto> loadedDashboards = Lists.newArrayList();
    org.sonar.persistence.dashboard.DashboardDto mainDashboard = null;
    for (DashboardTemplate dashboardTemplate : dashboardTemplates) {
      Dashboard dashboard = dashboardTemplate.createDashboard();
      if (shouldBeLoaded(dashboard)) {
        org.sonar.persistence.dashboard.DashboardDto dashboardDataModel = loadDashboard(dashboard);
        if (MAIN_DASHBOARD_ID.equals(dashboard.getId())) {
          mainDashboard = dashboardDataModel;
        } else {
          loadedDashboards.add(dashboardDataModel);
        }
      }
    }
    // and activate them
    activateDashboards(loadedDashboards, mainDashboard);

    profiler.stop();
  }

  protected void activateDashboards(List<org.sonar.persistence.dashboard.DashboardDto> loadedDashboards,
      org.sonar.persistence.dashboard.DashboardDto mainDashboard) {
    int nextOrderIndex = 0;
    if (mainDashboard != null) {
      activateDashboard(mainDashboard, 1);
      nextOrderIndex = 2;
    } else {
      nextOrderIndex = activeDashboardDao.selectMaxOrderIndexForNullUser() + 1;
    }
    Collections.sort(loadedDashboards, new DashboardComparator());
    for (org.sonar.persistence.dashboard.DashboardDto dashboardDto : loadedDashboards) {
      activateDashboard(dashboardDto, nextOrderIndex++);
    }
  }

  private void activateDashboard(org.sonar.persistence.dashboard.DashboardDto dashboardDto, int index) {
    ActiveDashboardDto activeDashboardDto = new ActiveDashboardDto();
    activeDashboardDto.setDashboardId(dashboardDto.getId());
    activeDashboardDto.setOrderIndex(index);
    activeDashboardDao.insert(activeDashboardDto);
    LOGGER.info("New dashboard '" + dashboardDto.getName() + "' registered and activated.");
  }

  protected org.sonar.persistence.dashboard.DashboardDto loadDashboard(Dashboard dashboard) {
    org.sonar.persistence.dashboard.DashboardDto dashboardDataModel = createDataModelFromExtension(dashboard);
    // save the new dashboard
    dashboardDao.insert(dashboardDataModel);
    // and save the fact that is has now already been loaded
    loadedTemplateDao.insert(new LoadedTemplateDto(dashboard.getId(), LoadedTemplateDto.DASHBOARD_TYPE));
    return dashboardDataModel;
  }

  protected org.sonar.persistence.dashboard.DashboardDto createDataModelFromExtension(Dashboard dashboard) {
    Date now = new Date();
    org.sonar.persistence.dashboard.DashboardDto dashboardDataModel = new org.sonar.persistence.dashboard.DashboardDto();
    dashboardDataModel.setKey(dashboard.getId());
    dashboardDataModel.setName(i18nManager.message(Locale.ENGLISH, "dashboard." + dashboard.getId() + ".name", dashboard.getName()));
    dashboardDataModel.setDescription(dashboard.getDescription());
    dashboardDataModel.setColumnLayout(dashboard.getLayout().toString());
    dashboardDataModel.setShared(true);
    dashboardDataModel.setCreatedAt(now);
    dashboardDataModel.setUpdatedAt(now);

    for (Widget widget : dashboard.getWidgets()) {
      org.sonar.persistence.dashboard.WidgetDto widgetDataModel = new org.sonar.persistence.dashboard.WidgetDto();
      widgetDataModel.setKey(widget.getId());
      widgetDataModel.setName(i18nManager.message(Locale.ENGLISH, "widget." + widget.getId() + ".name", ""));
      widgetDataModel.setColumnIndex(widget.getColumnIndex());
      widgetDataModel.setRowIndex(widget.getRowIndex());
      widgetDataModel.setConfigured(true);
      widgetDataModel.setCreatedAt(now);
      widgetDataModel.setUpdatedAt(now);
      dashboardDataModel.addWidget(widgetDataModel);

      for (Entry<String, String> property : widget.getProperties().entrySet()) {
        org.sonar.persistence.dashboard.WidgetPropertyDto widgetPropertyDataModel = new org.sonar.persistence.dashboard.WidgetPropertyDto();
        widgetPropertyDataModel.setKey(property.getKey());
        widgetPropertyDataModel.setValue(property.getValue());
        widgetDataModel.addWidgetProperty(widgetPropertyDataModel);
      }
    }

    return dashboardDataModel;
  }

  protected boolean shouldBeLoaded(Dashboard dashboard) {
    return loadedTemplateDao.selectByKeyAndType(dashboard.getId(), LoadedTemplateDto.DASHBOARD_TYPE) == null;
  }

  protected static class DashboardComparator implements Comparator<org.sonar.persistence.dashboard.DashboardDto> {

    public int compare(org.sonar.persistence.dashboard.DashboardDto d1, org.sonar.persistence.dashboard.DashboardDto d2) {
      return d1.getName().compareTo(d2.getName());
    }

  }

}
