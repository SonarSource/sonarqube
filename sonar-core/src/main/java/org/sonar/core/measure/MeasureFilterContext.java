/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.measure;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.core.resource.SnapshotDto;

class MeasureFilterContext {
  private Long userId;
  private SnapshotDto baseSnapshot;
  private String sql;
  private String json;

  Long getUserId() {
    return userId;
  }

  MeasureFilterContext setUserId(Long userId) {
    this.userId = userId;
    return this;
  }

  SnapshotDto getBaseSnapshot() {
    return baseSnapshot;
  }

  MeasureFilterContext setBaseSnapshot(SnapshotDto baseSnapshot) {
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

  String getJson() {
    return json;
  }

  MeasureFilterContext setJson(String json) {
    this.json = json;
    return this;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
      .append("json", json)
      .append("sql", sql)
      .append("user", userId)
      .toString();
  }
}
