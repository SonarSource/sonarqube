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

import java.util.Collection;
import java.util.List;
import javax.annotation.CheckForNull;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

public interface WidgetPropertyMapper {

  String COLUMNS = "wp.id, wp.widget_id as \"widgetId\", wp.kee as \"propertyKey\", wp.text_value as \"textValue\"";

  @Insert("insert into widget_properties (widget_id, kee, text_value) values (#{widgetId}, #{propertyKey}, #{textValue})")
  @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
  void insert(WidgetPropertyDto dto);

  @CheckForNull
  @Select("select " + COLUMNS + " from widget_properties wp where wp.id=#{id}")
  WidgetPropertyDto selectById(long propertyId);

  @Select("select " + COLUMNS + " from widget_properties wp " +
    "inner join widgets w on w.id=wp.widget_id where w.dashboard_id=#{id}")
  Collection<WidgetPropertyDto> selectByDashboard(long dashboardKey);

  void deleteByWidgetIds(List<Long> widgetIds);
}
