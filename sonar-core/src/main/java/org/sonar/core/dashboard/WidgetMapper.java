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
import org.apache.ibatis.annotations.Update;

import java.util.Collection;

public interface WidgetMapper {

  @Insert("insert into widgets (dashboard_id, widget_key, name, description, column_index, " +
    " row_index, configured, created_at, updated_at, resource_id)" +
    " values (#{dashboardId,jdbcType=INTEGER}, #{widgetKey,jdbcType=VARCHAR}, #{name,jdbcType=VARCHAR}, " +
    " #{description,jdbcType=VARCHAR}, #{columnIndex,jdbcType=INTEGER}, " +
    " #{rowIndex,jdbcType=INTEGER}, #{configured,jdbcType=BOOLEAN}, #{createdAt,jdbcType=TIMESTAMP}, #{updatedAt,jdbcType=TIMESTAMP}, #{resourceId,jdbcType=INTEGER})")
  @Options(keyColumn = "id", useGeneratedKeys = true, keyProperty = "id")
  void insert(WidgetDto widgetDto);

  WidgetDto selectById(long widgetId);

  Collection<WidgetDto> selectByDashboard(long dashboardKey);

  Collection<WidgetDto> selectAll();

  @Update("UPDATE widgets SET " +
    "dashboard_id=#{dashboardId,jdbcType=INTEGER}, " +
    "widget_key=#{widgetKey,jdbcType=VARCHAR}, " +
    "name=#{name,jdbcType=VARCHAR}, " +
    "description=#{description,jdbcType=VARCHAR}, " +
    "column_index=#{columnIndex,jdbcType=INTEGER}, " +
    "row_index=#{rowIndex,jdbcType=INTEGER}, " +
    "configured=#{configured,jdbcType=BOOLEAN}, " +
    "created_at=#{createdAt,jdbcType=TIMESTAMP}, " +
    "updated_at=#{updatedAt,jdbcType=TIMESTAMP}, " +
    "resource_id=#{resourceId,jdbcType=INTEGER} " +
    "WHERE id=#{id}")
  @Options(useGeneratedKeys = false)
  void update(WidgetDto item);
}
