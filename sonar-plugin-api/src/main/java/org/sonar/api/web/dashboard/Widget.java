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
package org.sonar.api.web.dashboard;

import java.util.Map;

import com.google.common.collect.Maps;

/**
 * 
 * Definition of a widget inside a dashboard.
 * 
 * @since 2.13
 */
public final class Widget {

  private String id;
  private int columnIndex;
  private int rowIndex;
  private Map<String, String> properties;

  Widget(String id, int columnIndex, int rowIndex) {
    this.id = id;
    this.columnIndex = columnIndex;
    this.rowIndex = rowIndex;
    this.properties = Maps.newHashMap();
  }

  /**
   * Adds a property to this widget.
   * 
   * @param key
   *          the id of the property
   * @param value
   *          the value of the property
   */
  public void addProperty(String key, String value) {
    properties.put(key, value);
  }

  /**
   * Returns the properties of this widget.
   * 
   * @return the properties
   */
  public Map<String, String> getProperties() {
    return properties;
  }

  /**
   * Returns the identifier of this widget.
   * 
   * @return the id
   */
  public String getId() {
    return id;
  }

  /**
   * Returns the column index of this widget.
   * 
   * @return the columnIndex
   */
  public int getColumnIndex() {
    return columnIndex;
  }

  /**
   * Returns the row index of this widget.
   * 
   * @return the rowIndex
   */
  public int getRowIndex() {
    return rowIndex;
  }

}
