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

import javax.annotation.CheckForNull;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface DashboardMapper {

  String COLUMNS = "id, user_id as \"userId\", name, description, column_layout as \"columnLayout\", " +
    "shared, is_global as \"global\", created_at as \"createdAt\", updated_at as \"updatedAt\"";

  @CheckForNull
  @Select("select " + COLUMNS + " from dashboards where id=#{id}")
  DashboardDto selectById(long id);

  @CheckForNull
  @Select("select " + COLUMNS + " from dashboards where id=#{id} and (shared=${_true} or user_id=${userId})")
  DashboardDto selectAllowedById(@Param("id") long id, @Param("userId") long userId);

  @CheckForNull
  @Select("select " + COLUMNS + " from dashboards WHERE name=#{id} and user_id is null")
  DashboardDto selectGlobalDashboard(String name);

  @Insert("INSERT INTO dashboards (user_id, name, description, column_layout, shared, is_global, created_at, " +
    "updated_at) VALUES (#{userId}, #{name}, #{description}, #{columnLayout}, #{shared}, " +
    "#{global}, #{createdAt}, #{updatedAt})")
  @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
  void insert(DashboardDto dashboardDto);
}
