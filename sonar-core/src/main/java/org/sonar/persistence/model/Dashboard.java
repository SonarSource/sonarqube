/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.persistence.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import com.google.common.collect.Lists;

public class Dashboard {

  private Long id;
  private String key;
  private Long userId;
  private String name;
  private String description;
  private String columnLayout;
  private boolean shared;
  private Date createdAt;
  private Date updatedAt;
  private ArrayList<Widget> widgets = Lists.newArrayList();

  /**
   * @return the id
   */
  public Long getId() {
    return id;
  }

  /**
   * @param id
   *          the id to set
   */
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * @return the key
   */
  public String getKey() {
    return key;
  }

  /**
   * @param key
   *          the key to set
   */
  public void setKey(String key) {
    this.key = key;
  }

  /**
   * @return the userId
   */
  public Long getUserId() {
    return userId;
  }

  /**
   * @param userId
   *          the userId to set
   */
  public void setUserId(Long userId) {
    this.userId = userId;
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @param name
   *          the name to set
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
   * @param description
   *          the description to set
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * @return the columnLayout
   */
  public String getColumnLayout() {
    return columnLayout;
  }

  /**
   * @param columnLayout
   *          the columnLayout to set
   */
  public void setColumnLayout(String columnLayout) {
    this.columnLayout = columnLayout;
  }

  /**
   * @return the shared
   */
  public boolean getShared() {
    return shared;
  }

  /**
   * @param shared
   *          the shared to set
   */
  public void setShared(boolean shared) {
    this.shared = shared;
  }

  /**
   * @return the createdAt
   */
  public Date getCreatedAt() {
    return createdAt;
  }

  /**
   * @param createdAt
   *          the createdAt to set
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
   * @param updatedAt
   *          the updatedAt to set
   */
  public void setUpdatedAt(Date updatedAt) {
    this.updatedAt = updatedAt;
  }

  /**
   * @return the widgets
   */
  public Collection<Widget> getWidgets() {
    return widgets;
  }

  /**
   * @param widget
   *          the widget to add
   */
  public void addWidget(Widget widget) {
    widgets.add(widget);
  }

}
