/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.dashboard;

public final class WidgetPropertyDto {
  private Long id;
  private Long widgetId;
  private String key;
  private String value;

  /**
   * @return the id
   */
  public Long getId() {
    return id;
  }

  /**
   * @param id the id to set
   */
  public WidgetPropertyDto setId(Long id) {
    this.id = id;
    return this;
  }

  /**
   * @return the widgetId
   */
  public Long getWidgetId() {
    return widgetId;
  }

  /**
   * @param widgetId the widgetId to set
   */
  public WidgetPropertyDto setWidgetId(Long widgetId) {
    this.widgetId = widgetId;
    return this;
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
  public WidgetPropertyDto setKey(String key) {
    this.key = key;
    return this;
  }

  /**
   * @return the value
   */
  public String getValue() {
    return value;
  }

  /**
   * @param value the value to set
   */
  public WidgetPropertyDto setValue(String value) {
    this.value = value;
    return this;
  }
}
