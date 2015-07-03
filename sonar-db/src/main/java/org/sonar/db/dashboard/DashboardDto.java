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

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import org.sonar.db.Dto;

public final class DashboardDto extends Dto<Long> {

  private Long id;
  private Long userId;
  private String name;
  private String description;
  private String columnLayout;
  private boolean shared;
  private boolean global;
  private List<WidgetDto> widgetDtos = Lists.newArrayList();

  public Long getId() {
    return id;
  }

  @Override
  public Long getKey() {
    return id;
  }

  public DashboardDto setId(Long id) {
    this.id = id;
    return this;
  }

  public Long getUserId() {
    return userId;
  }

  public DashboardDto setUserId(Long userId) {
    this.userId = userId;
    return this;
  }

  public String getName() {
    return name;
  }

  public DashboardDto setName(String name) {
    this.name = name;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public DashboardDto setDescription(String description) {
    this.description = description;
    return this;
  }

  public String getColumnLayout() {
    return columnLayout;
  }

  public DashboardDto setColumnLayout(String columnLayout) {
    this.columnLayout = columnLayout;
    return this;
  }

  public boolean getShared() {
    return shared;
  }

  public DashboardDto setShared(boolean shared) {
    this.shared = shared;
    return this;
  }

  public boolean getGlobal() {
    return global;
  }

  public DashboardDto setGlobal(boolean global) {
    this.global = global;
    return this;
  }

  public Collection<WidgetDto> getWidgets() {
    return widgetDtos;
  }

  public DashboardDto addWidget(WidgetDto widgetDto) {
    widgetDtos.add(widgetDto);
    return this;
  }

}
