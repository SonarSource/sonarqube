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
package org.sonar.server.measure;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.core.component.SnapshotDto;

import javax.annotation.Nullable;

class MeasureFilterContext {
  private Long userId = null;
  private SnapshotDto baseSnapshot = null;
  private String sql;
  private String data;

  Long getUserId() {
    return userId;
  }

  MeasureFilterContext setUserId(@Nullable Long userId) {
    this.userId = userId;
    return this;
  }

  SnapshotDto getBaseSnapshot() {
    return baseSnapshot;
  }

  MeasureFilterContext setBaseSnapshot(@Nullable SnapshotDto baseSnapshot) {
    this.baseSnapshot = baseSnapshot;
    return this;
  }

  String getSql() {
    return sql;
  }

  MeasureFilterContext setSql(String sql) {
    this.sql = sql;
    return this;
  }

  String getData() {
    return data;
  }

  MeasureFilterContext setData(String data) {
    this.data = data;
    return this;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
      .append("filter", data)
      .append("sql", sql)
      .append("user", userId)
      .toString();
  }
}
