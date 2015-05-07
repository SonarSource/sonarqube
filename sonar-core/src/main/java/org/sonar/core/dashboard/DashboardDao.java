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
package org.sonar.core.dashboard;

import org.apache.ibatis.session.SqlSession;
import org.sonar.api.BatchSide;
import org.sonar.api.ServerSide;
import org.sonar.core.persistence.DaoComponent;
import org.sonar.core.persistence.MyBatis;

@BatchSide
@ServerSide
public class DashboardDao implements DaoComponent {

  private MyBatis mybatis;

  public DashboardDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  public DashboardDto selectGlobalDashboard(String name) {
    SqlSession session = mybatis.openSession(false);
    try {
      DashboardMapper mapper = session.getMapper(DashboardMapper.class);
      return mapper.selectGlobalDashboard(name);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void insert(DashboardDto dashboardDto) {
    SqlSession session = mybatis.openSession(false);
    DashboardMapper dashboardMapper = session.getMapper(DashboardMapper.class);
    WidgetMapper widgetMapper = session.getMapper(WidgetMapper.class);
    WidgetPropertyMapper widgetPropertyMapper = session.getMapper(WidgetPropertyMapper.class);
    try {
      dashboardMapper.insert(dashboardDto);
      for (WidgetDto widgetDto : dashboardDto.getWidgets()) {
        widgetDto.setDashboardId(dashboardDto.getId());
        widgetMapper.insert(widgetDto);
        for (WidgetPropertyDto widgetPropertyDto : widgetDto.getWidgetProperties()) {
          widgetPropertyDto.setWidgetId(widgetDto.getId());
          widgetPropertyMapper.insert(widgetPropertyDto);
        }
      }
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

}
