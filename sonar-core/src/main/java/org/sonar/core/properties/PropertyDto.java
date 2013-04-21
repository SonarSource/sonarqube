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
package org.sonar.core.properties;

public final class PropertyDto {
  private Long id;
  private String key;
  private String value;
  private Long resourceId;
  private Long userId;

  public Long getId() {
    return id;
  }

  public PropertyDto setId(Long id) {
    this.id = id;
    return this;
  }

  public String getKey() {
    return key;
  }

  public PropertyDto setKey(String key) {
    this.key = key;
    return this;
  }

  public String getValue() {
    return value;
  }

  public PropertyDto setValue(String value) {
    this.value = value;
    return this;
  }

  public Long getResourceId() {
    return resourceId;
  }

  public PropertyDto setResourceId(Long resourceId) {
    this.resourceId = resourceId;
    return this;
  }

  public Long getUserId() {
    return userId;
  }

  public PropertyDto setUserId(Long userId) {
    this.userId = userId;
    return this;
  }
}
