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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import java.util.Collection;

public class WidgetPropertyDto {
  private Long id;
  private Long widgetId;
  private String propertyKey;
  private String textValue;

  public Long getId() {
    return id;
  }

  public WidgetPropertyDto setId(Long id) {
    this.id = id;
    return this;
  }

  public Long getWidgetId() {
    return widgetId;
  }

  public WidgetPropertyDto setWidgetId(Long widgetId) {
    this.widgetId = widgetId;
    return this;
  }

  public String getPropertyKey() {
    return propertyKey;
  }

  public WidgetPropertyDto setPropertyKey(String s) {
    this.propertyKey = s;
    return this;
  }

  public String getTextValue() {
    return textValue;
  }

  public WidgetPropertyDto setTextValue(String s) {
    this.textValue = s;
    return this;
  }

  public static ListMultimap<Long, WidgetPropertyDto> groupByWidgetId(Collection<WidgetPropertyDto> properties) {
    ListMultimap<Long, WidgetPropertyDto> group = ArrayListMultimap.create();
    for (WidgetPropertyDto property : properties) {
      group.put(property.getWidgetId(), property);
    }
    return group;
  }
}
