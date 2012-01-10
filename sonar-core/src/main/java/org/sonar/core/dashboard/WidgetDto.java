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
package org.sonar.core.dashboard;

import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.Date;
import java.util.List;

public final class WidgetDto {

  private Long id;
  private Long dashboardId;
  private String key;
  private String name;
  private String description;
  private Integer columnIndex;
  private Integer rowIndex;
  private boolean configured;
  private Date createdAt;
  private Date updatedAt;
  private List<WidgetPropertyDto> widgetPropertyDtos = Lists.newArrayList();

  /**
   * @return the id
   */
  public Long getId() {
    return id;
  }

  /**
   * @param id the id to set
   */
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * @return the dashboardId
   */
  public Long getDashboardId() {
    return dashboardId;
  }

  /**
   * @param dashboardId the dashboardId to set
   */
  public void setDashboardId(Long dashboardId) {
    this.dashboardId = dashboardId;
  }

  /**
   * @return the key
   */
  public String getKey() {
    return key;
  }

  /**
   * @param key the key to set
   */
  public void setKey(String key) {
    this.key = key;
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @param name the name to set
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * @param description the description to set
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * @return the columnIndex
   */
  public Integer getColumnIndex() {
    return columnIndex;
  }

  /**
   * @param columnIndex the columnIndex to set
   */
  public void setColumnIndex(Integer columnIndex) {
    this.columnIndex = columnIndex;
  }

  /**
   * @return the rowIndex
   */
  public Integer getRowIndex() {
    return rowIndex;
  }

  /**
   * @param rowIndex the rowIndex to set
   */
  public void setRowIndex(Integer rowIndex) {
    this.rowIndex = rowIndex;
  }

  /**
   * @return the configured
   */
  public boolean getConfigured() {
    return configured;
  }

  /**
   * @param configured the configured to set
   */
  public void setConfigured(boolean configured) {
    this.configured = configured;
  }

  /**
   * @return the createdAt
   */
  public Date getCreatedAt() {
    return createdAt;
  }

  /**
   * @param createdAt the createdAt to set
   */
  public void setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
  }

  /**
   * @return the updatedAt
   */
  public Date getUpdatedAt() {
    return updatedAt;
  }

  /**
   * @param updatedAt the updatedAt to set
   */
  public void setUpdatedAt(Date updatedAt) {
    this.updatedAt = updatedAt;
  }

  /**
   * @return the widgetProperties
   */
  public Collection<WidgetPropertyDto> getWidgetProperties() {
    return widgetPropertyDtos;
  }

  /**
   * @param widgetPropertyDto the widgetProperty to set
   */
  public void addWidgetProperty(WidgetPropertyDto widgetPropertyDto) {
    widgetPropertyDtos.add(widgetPropertyDto);
  }

}
