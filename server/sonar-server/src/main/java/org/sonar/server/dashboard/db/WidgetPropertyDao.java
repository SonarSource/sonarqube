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

import org.sonar.api.utils.System2;
import org.sonar.core.dashboard.WidgetPropertyDto;
import org.sonar.core.dashboard.WidgetPropertyMapper;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.BaseDao;

import java.util.Collection;
import java.util.List;

public class WidgetPropertyDao extends BaseDao<WidgetPropertyMapper, WidgetPropertyDto, Long> {

  public WidgetPropertyDao(System2 system2) {
    super(WidgetPropertyMapper.class, system2);
  }

  @Override
  protected WidgetPropertyDto doInsert(DbSession session, WidgetPropertyDto item) {
    mapper(session).insert(item);
    return item;
  }

  @Override
  protected WidgetPropertyDto doGetNullableByKey(DbSession session, Long propertyId) {
    return mapper(session).selectById(propertyId);
  }

  public Collection<WidgetPropertyDto> findByDashboard(DbSession session, long dashboardKey) {
    return mapper(session).selectByDashboard(dashboardKey);
  }

  public void deleteByWidgetIds(DbSession session, List<Long> widgetIdsWithPropertiesToDelete) {
    mapper(session).deleteByWidgetIds(widgetIdsWithPropertiesToDelete);
  }

}
