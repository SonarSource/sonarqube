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
package org.sonar.db.dashboard;

import com.google.common.base.Function;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.sonar.db.Dao;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;

public class WidgetPropertyDao implements Dao {

  private final MyBatis myBatis;

  public WidgetPropertyDao(MyBatis myBatis) {
    this.myBatis = myBatis;
  }

  public WidgetPropertyDto insert(WidgetPropertyDto item) {
    DbSession session = myBatis.openSession(false);
    try {
      return insert(session, item);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public WidgetPropertyDto insert(DbSession session, WidgetPropertyDto item) {
    mapper(session).insert(item);
    return item;
  }

  public void insert(DbSession session, Collection<WidgetPropertyDto> items) {
    for (WidgetPropertyDto item : items) {
      insert(session, item);
    }
  }

  public WidgetPropertyDto selectByKey(Long propertyId) {
    DbSession session = myBatis.openSession(false);
    try {
      return selectByKey(session, propertyId);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public WidgetPropertyDto selectByKey(DbSession session, Long propertyId) {
    return mapper(session).selectById(propertyId);
  }

  public Collection<WidgetPropertyDto> selectByDashboard(DbSession session, long dashboardKey) {
    return mapper(session).selectByDashboard(dashboardKey);
  }

  public void deleteByWidgetIds(final DbSession session, List<Long> widgetIdsWithPropertiesToDelete) {
    DatabaseUtils.executeLargeInputs(widgetIdsWithPropertiesToDelete, new Function<List<Long>, List<Void>>() {
      @Override
      public List<Void> apply(List<Long> input) {
        mapper(session).deleteByWidgetIds(input);
        return Arrays.asList();
      }
    });
  }

  private static WidgetPropertyMapper mapper(DbSession session) {
    return session.getMapper(WidgetPropertyMapper.class);
  }
}
