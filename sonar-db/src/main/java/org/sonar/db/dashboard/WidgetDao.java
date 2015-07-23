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
package org.sonar.db.dashboard;

import java.util.Collection;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;

public class WidgetDao implements Dao {

  private MyBatis myBatis;

  public WidgetDao(MyBatis myBatis) {
    this.myBatis = myBatis;
  }

  public WidgetDto selectByKey(Long widgetId) {
    DbSession session = myBatis.openSession(false);
    try {
      return selectByKey(session, widgetId);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public WidgetDto selectByKey(DbSession session, Long widgetId) {
    return mapper(session).selectById(widgetId);
  }

  public WidgetDto update(WidgetDto item) {
    DbSession session = myBatis.openSession(false);
    try {
      return update(session, item);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public WidgetDto update(DbSession session, WidgetDto item) {
    mapper(session).update(item);
    return item;
  }

  public Collection<WidgetDto> findByDashboard(DbSession session, long dashboardKey) {
    return mapper(session).selectByDashboard(dashboardKey);
  }

  public Collection<WidgetDto> findAll(DbSession session) {
    return mapper(session).selectAll();
  }

  private WidgetMapper mapper(DbSession session) {
    return session.getMapper(WidgetMapper.class);
  }

}
