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

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;

public interface WidgetMapper {

  String COLUMNS = "ID, DASHBOARD_ID as \"dashboardId\", WIDGET_KEY as \"widgetKey\", NAME, DESCRIPTION, " +
    "COLUMN_INDEX as \"columnIndex\", ROW_INDEX as \"rowIndex\", CONFIGURED, CREATED_AT as \"createdAt\", " +
    "UPDATED_AT as \"updatedAt\", RESOURCE_ID as \"resourceId\"";

  @Insert("insert into widgets (dashboard_id, widget_key, name, description, column_index, " +
    " row_index, configured, created_at, updated_at, resource_id)" +
    " values (#{dashboardId}, #{widgetKey}, #{name}, #{description}, #{columnIndex}, " +
    " #{rowIndex}, #{configured}, #{createdAt}, #{updatedAt}, #{resourceId})")
  @Options(keyColumn = "id", useGeneratedKeys = true, keyProperty = "id")
  void insert(WidgetDto widgetDto);

  @Select("select " + COLUMNS + " from widgets where id=#{id}")
  WidgetDto selectById(long widgetId);

  @Select("select " + COLUMNS + " from widgets where dashboard_id=#{id}")
  Collection<WidgetDto> selectByDashboard(long dashboardKey);

  @Select("select " + COLUMNS + " from widgets")
  Collection<WidgetDto> selectAll();

  @Update("UPDATE widgets SET " +
    "dashboard_id=#{dashboardId}, " +
    "widget_key=#{widgetKey}, " +
    "name=#{name}, " +
    "description=#{description}, " +
    "column_index=#{columnIndex}, " +
    "row_index=#{rowIndex}, " +
    "configured=#{configured}, " +
    "created_at=#{createdAt}, " +
    "updated_at=#{updatedAt}, " +
    "resource_id=#{resourceId} " +
    "WHERE id=#{id}")
  void update(WidgetDto item);
}
