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
package org.sonar.persistence.dashboard;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.sonar.persistence.DaoTestCase;
import org.sonar.persistence.dashboard.DashboardDao;
import org.sonar.persistence.dashboard.DashboardDto;
import org.sonar.persistence.dashboard.WidgetDto;
import org.sonar.persistence.dashboard.WidgetPropertyDto;

public class DashboardDaoTest extends DaoTestCase {

  private DashboardDao dao;

  @Before
  public void createDao() throws Exception {
    dao = new DashboardDao(getMyBatis());
  }

  @Test
  public void shouldInsert() throws Exception {
    setupData("shouldInsert");
    Date aDate = new Date();

    DashboardDto dashboardDto = new DashboardDto();
    dashboardDto.setKey("d-key");
    dashboardDto.setUserId(6L);
    dashboardDto.setName("My Dashboard");
    dashboardDto.setDescription("This is a dashboard");
    dashboardDto.setColumnLayout("100%");
    dashboardDto.setShared(true);
    dashboardDto.setCreatedAt(aDate);
    dashboardDto.setUpdatedAt(aDate);

    WidgetDto widgetDto = new WidgetDto();
    widgetDto.setKey("code_coverage");
    widgetDto.setName("Code coverage");
    widgetDto.setDescription("Widget for code coverage");
    widgetDto.setColumnIndex(13);
    widgetDto.setRowIndex(14);
    widgetDto.setConfigured(true);
    widgetDto.setCreatedAt(aDate);
    widgetDto.setUpdatedAt(aDate);
    dashboardDto.addWidget(widgetDto);

    WidgetPropertyDto property = new WidgetPropertyDto();
    property.setKey("displayITs");
    property.setValue("true");
    property.setValueType("BOOLEAN");
    widgetDto.addWidgetProperty(property);

    dao.insert(dashboardDto);

    checkTables("shouldInsert", new String[] { "created_at", "updated_at" }, "dashboards", "widgets", "widget_properties");
  }

  @Test
  public void shouldInsertWithNullableColumns() throws Exception {
    setupData("shouldInsert");

    DashboardDto dashboardDto = new DashboardDto();
    dashboardDto.setKey("d-key");
    dashboardDto.setUserId(null);
    dashboardDto.setName(null);
    dashboardDto.setDescription(null);
    dashboardDto.setColumnLayout(null);
    dashboardDto.setShared(true);
    dashboardDto.setCreatedAt(null);
    dashboardDto.setUpdatedAt(null);

    WidgetDto widgetDto = new WidgetDto();
    widgetDto.setKey("code_coverage");
    widgetDto.setName(null);
    widgetDto.setDescription(null);
    widgetDto.setColumnIndex(null);
    widgetDto.setRowIndex(null);
    widgetDto.setConfigured(true);
    widgetDto.setCreatedAt(null);
    widgetDto.setUpdatedAt(null);
    dashboardDto.addWidget(widgetDto);

    WidgetPropertyDto property = new WidgetPropertyDto();
    property.setKey(null);
    property.setValue(null);
    property.setValueType(null);
    widgetDto.addWidgetProperty(property);

    dao.insert(dashboardDto);

    checkTables("shouldInsertWithNullableColumns", "dashboards", "widgets", "widget_properties");
  }

}
