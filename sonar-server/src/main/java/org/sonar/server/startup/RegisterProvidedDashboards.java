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
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.api.web.Dashboard;
import org.sonar.api.web.DashboardWidget;
import org.sonar.api.web.DashboardWidgets;
import org.sonar.api.web.WidgetProperty;
import org.sonar.core.i18n.I18nManager;
import org.sonar.persistence.dao.ActiveDashboardDao;
import org.sonar.persistence.dao.DashboardDao;
import org.sonar.persistence.dao.LoadedTemplateDao;
import org.sonar.persistence.model.ActiveDashboard;
import org.sonar.persistence.model.LoadedTemplate;
import org.sonar.persistence.model.Widget;

import com.google.common.collect.Lists;

public final class RegisterProvidedDashboards {

  private static final Logger LOGGER = LoggerFactory.getLogger(RegisterProvidedDashboards.class);
  private static final String MAIN_DASHBOARD_ID = "sonar-main-dashboard";

  private ArrayList<Dashboard> dashboards;
  private DashboardDao dashboardDao;
  private ActiveDashboardDao activeDashboardDao;
  private LoadedTemplateDao loadedTemplateDao;
  private I18nManager i18nManager;

  public RegisterProvidedDashboards(DashboardDao dashboardDao, ActiveDashboardDao activeDashboardDao, LoadedTemplateDao loadedTemplateDao,
      I18nManager i18nManager) {
    this(new Dashboard[] {}, dashboardDao, activeDashboardDao, loadedTemplateDao, i18nManager);
  }

  public RegisterProvidedDashboards(Dashboard[] dashboardArray, DashboardDao dashboardDao, ActiveDashboardDao activeDashboardDao,
      LoadedTemplateDao loadedTemplateDao, I18nManager i18nManager) {
    this.dashboards = Lists.newArrayList(dashboardArray);
    this.dashboardDao = dashboardDao;
    this.activeDashboardDao = activeDashboardDao;
    this.loadedTemplateDao = loadedTemplateDao;
    this.i18nManager = i18nManager;
  }

  public void start() {
    TimeProfiler profiler = new TimeProfiler().start("Load provided dashboards");

    // load the dashboards that need to be loaded
    ArrayList<org.sonar.persistence.model.Dashboard> loadedDashboards = Lists.newArrayList();
    org.sonar.persistence.model.Dashboard mainDashboard = null;
    for (Dashboard dashboard : dashboards) {
      if (shouldBeLoaded(dashboard)) {
        org.sonar.persistence.model.Dashboard dashboardDataModel = loadDashboard(dashboard);
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

  protected void activateDashboards(ArrayList<org.sonar.persistence.model.Dashboard> loadedDashboards,
      org.sonar.persistence.model.Dashboard mainDashboard) {
    int nextOrderIndex = 0;
    if (mainDashboard != null) {
      activateDashboard(mainDashboard, 1);
      nextOrderIndex = 2;
    } else {
      nextOrderIndex = activeDashboardDao.selectMaxOrderIndexForNullUser() + 1;
    }
    Collections.sort(loadedDashboards, new DashboardComparator());
    for (org.sonar.persistence.model.Dashboard dashboard : loadedDashboards) {
      activateDashboard(dashboard, nextOrderIndex++);
    }
  }

  private void activateDashboard(org.sonar.persistence.model.Dashboard dashboard, int index) {
    ActiveDashboard activeDashboard = new ActiveDashboard();
    activeDashboard.setDashboardId(dashboard.getId());
    activeDashboard.setOrderIndex(index);
    activeDashboardDao.insert(activeDashboard);
    LOGGER.info("New dashboard '" + dashboard.getName() + "' registered and activated.");
  }

  protected org.sonar.persistence.model.Dashboard loadDashboard(Dashboard dashboard) {
    org.sonar.persistence.model.Dashboard dashboardDataModel = createDataModelFromExtension(dashboard);
    // save the new dashboard
    dashboardDao.insert(dashboardDataModel);
    // and save the fact that is has now already been loaded
    loadedTemplateDao.insert(new LoadedTemplate(dashboard.getId(), LoadedTemplate.DASHBOARD_TYPE));
    return dashboardDataModel;
  }

  protected org.sonar.persistence.model.Dashboard createDataModelFromExtension(Dashboard dashboard) {
    Date now = new Date();
    org.sonar.persistence.model.Dashboard dashboardDataModel = new org.sonar.persistence.model.Dashboard();
    dashboardDataModel.setKey(dashboard.getId());
    dashboardDataModel.setName(i18nManager.message(Locale.ENGLISH, "dashboard." + dashboard.getId() + ".name", dashboard.getName()));
    dashboardDataModel.setDescription(dashboard.getDescription());
    dashboardDataModel.setColumnLayout(dashboard.getLayout());
    dashboardDataModel.setShared(true);
    dashboardDataModel.setCreatedAt(now);
    dashboardDataModel.setUpdatedAt(now);

    DashboardWidgets dashboardWidgets = dashboard.getClass().getAnnotation(DashboardWidgets.class);
    if (dashboardWidgets != null) {
      for (DashboardWidget dashboardWidget : dashboardWidgets.value()) {
        Widget widget = new Widget();
        widget.setKey(dashboardWidget.id());
        widget.setName(i18nManager.message(Locale.ENGLISH, "widget." + dashboardWidget.id() + ".name", ""));
        widget.setColumnIndex(dashboardWidget.columnIndex());
        widget.setRowIndex(dashboardWidget.rowIndex());
        widget.setConfigured(true);
        widget.setCreatedAt(now);
        widget.setUpdatedAt(now);
        dashboardDataModel.addWidget(widget);

        WidgetProperty[] dashboardWidgetProperties = dashboardWidget.properties();
        for (int i = 0; i < dashboardWidgetProperties.length; i++) {
          WidgetProperty dashboardWidgetProperty = dashboardWidgetProperties[i];
          org.sonar.persistence.model.WidgetProperty widgetProperty = new org.sonar.persistence.model.WidgetProperty();
          widgetProperty.setKey(dashboardWidgetProperty.key());
          widgetProperty.setValue(dashboardWidgetProperty.defaultValue());
          widgetProperty.setValueType(dashboardWidgetProperty.type().toString());
          widget.addWidgetProperty(widgetProperty);
        }
      }
    }

    return dashboardDataModel;
  }

  protected boolean shouldBeLoaded(Dashboard dashboard) {
    return loadedTemplateDao.selectByKeyAndType(dashboard.getId(), LoadedTemplate.DASHBOARD_TYPE) == null;
  }

  protected static class DashboardComparator implements Comparator<org.sonar.persistence.model.Dashboard> {

    public int compare(org.sonar.persistence.model.Dashboard d1, org.sonar.persistence.model.Dashboard d2) {
      return d1.getName().compareTo(d2.getName());
    }

  }

}
