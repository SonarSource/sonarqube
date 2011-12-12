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
package org.sonar.persistence.dao;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.sonar.persistence.model.Dashboard;
import org.sonar.persistence.model.Widget;
import org.sonar.persistence.model.WidgetProperty;

public class DashboardDaoTest extends DaoTestCase {

  private DashboardDao dao;

  @Before
  public void createDao() throws Exception {
    dao = new DashboardDao(getMyBatis());
  }

  @Test
  public void shouldInsert() throws Exception {
    setupData("shouldInsert");
    Date aDate = new Date(123456789);

    Dashboard dashboard = new Dashboard();
    dashboard.setKey("d-key");
    dashboard.setUserId(6L);
    dashboard.setName("My Dashboard");
    dashboard.setDescription("This is a dashboard");
    dashboard.setColumnLayout("100%");
    dashboard.setShared(true);
    dashboard.setCreatedAt(aDate);
    dashboard.setUpdatedAt(aDate);

    Widget widget = new Widget();
    widget.setKey("code_coverage");
    widget.setName("Code coverage");
    widget.setDescription("Widget for code coverage");
    widget.setColumnIndex(13);
    widget.setRowIndex(14);
    widget.setConfigured(true);
    widget.setCreatedAt(aDate);
    widget.setUpdatedAt(aDate);
    dashboard.addWidget(widget);

    WidgetProperty property = new WidgetProperty();
    property.setKey("displayITs");
    property.setValue("true");
    property.setValueType("BOOLEAN");
    widget.addWidgetProperty(property);

    dao.insert(dashboard);

    checkTables("shouldInsert", "dashboards", "widgets", "widget_properties");
  }

  @Test
  public void shouldInsertWithNullableColumns() throws Exception {
    setupData("shouldInsert");
    Date aDate = new Date(123456789);

    Dashboard dashboard = new Dashboard();
    dashboard.setKey("d-key");
    dashboard.setUserId(null);
    dashboard.setName(null);
    dashboard.setDescription(null);
    dashboard.setColumnLayout(null);
    dashboard.setShared(true);
    dashboard.setCreatedAt(null);
    dashboard.setUpdatedAt(null);

    Widget widget = new Widget();
    widget.setKey("code_coverage");
    widget.setName(null);
    widget.setDescription(null);
    widget.setColumnIndex(null);
    widget.setRowIndex(null);
    widget.setConfigured(true);
    widget.setCreatedAt(null);
    widget.setUpdatedAt(null);
    dashboard.addWidget(widget);

    WidgetProperty property = new WidgetProperty();
    property.setKey(null);
    property.setValue(null);
    property.setValueType(null);
    widget.addWidgetProperty(property);

    dao.insert(dashboard);

    checkTables("shouldInsertWithNullableColumns", "dashboards", "widgets", "widget_properties");
  }

}
