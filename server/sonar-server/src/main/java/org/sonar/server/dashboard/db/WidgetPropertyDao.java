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
package org.sonar.server.dashboard.db;

import com.google.common.base.Function;
import org.sonar.core.dashboard.WidgetPropertyDto;
import org.sonar.core.dashboard.WidgetPropertyMapper;
import org.sonar.core.persistence.DaoComponent;
import org.sonar.core.persistence.DaoUtils;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class WidgetPropertyDao implements DaoComponent {

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

  public WidgetPropertyDto getNullableByKey(Long propertyId) {
    DbSession session = myBatis.openSession(false);
    try {
      return getNullableByKey(session, propertyId);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public WidgetPropertyDto getNullableByKey(DbSession session, Long propertyId) {
    return mapper(session).selectById(propertyId);
  }

  public Collection<WidgetPropertyDto> findByDashboard(DbSession session, long dashboardKey) {
    return mapper(session).selectByDashboard(dashboardKey);
  }

  public void deleteByWidgetIds(final DbSession session, List<Long> widgetIdsWithPropertiesToDelete) {
    DaoUtils.executeLargeInputs(widgetIdsWithPropertiesToDelete, new Function<List<Long>, List<Void>>() {
      @Override
      public List<Void> apply(List<Long> input) {
        mapper(session).deleteByWidgetIds(input);
        return Arrays.asList();
      }
    });
  }

  private WidgetPropertyMapper mapper(DbSession session) {
    return session.getMapper(WidgetPropertyMapper.class);
  }
}
