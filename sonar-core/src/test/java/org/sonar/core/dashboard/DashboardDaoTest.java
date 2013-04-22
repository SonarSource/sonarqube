/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.core.dashboard;

import org.junit.Before;
import org.junit.Test;
import org.sonar.core.persistence.AbstractDaoTestCase;

import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class DashboardDaoTest extends AbstractDaoTestCase {

  private DashboardDao dao;

  @Before
  public void createDao() {
    dao = new DashboardDao(getMyBatis());
  }

  @Test
  public void shouldSelectGlobalDashboard() {
    setupData("shouldSelectGlobalDashboard");
    DashboardDto dashboard = dao.selectGlobalDashboard("SQALE");
    assertThat(dashboard.getId(), is(2L));
    assertThat(dashboard.getUserId(), nullValue());

    assertNull(dao.selectGlobalDashboard("unknown"));
  }

  @Test
  public void shouldInsert() {
    setupData("shouldInsert");
    Date aDate = new Date();

    DashboardDto dashboardDto = new DashboardDto();
    dashboardDto.setUserId(6L);
    dashboardDto.setName("My Dashboard");
    dashboardDto.setDescription("This is a dashboard");
    dashboardDto.setColumnLayout("100%");
    dashboardDto.setShared(true);
    dashboardDto.setGlobal(true);
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
    widgetDto.addWidgetProperty(property);

    dao.insert(dashboardDto);

    checkTables("shouldInsert", new String[]{"created_at", "updated_at"}, "dashboards", "widgets", "widget_properties");
  }

  @Test
  public void shouldInsertWithNullableColumns() throws Exception {
    setupData("shouldInsert");

    DashboardDto dashboardDto = new DashboardDto();
    dashboardDto.setUserId(null);
    dashboardDto.setName(null);
    dashboardDto.setDescription(null);
    dashboardDto.setColumnLayout(null);
    dashboardDto.setShared(true);
    dashboardDto.setGlobal(false);
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
    widgetDto.addWidgetProperty(property);

    dao.insert(dashboardDto);

    checkTables("shouldInsertWithNullableColumns", "dashboards", "widgets", "widget_properties");
  }

}
