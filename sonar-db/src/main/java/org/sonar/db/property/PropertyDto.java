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
package org.sonar.db.property;

import com.google.common.base.Objects;

public class PropertyDto {
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

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final PropertyDto other = (PropertyDto) obj;

    return Objects.equal(this.key, other.key)
      && Objects.equal(this.value, other.value)
      && Objects.equal(this.userId, other.userId)
      && Objects.equal(this.resourceId, other.resourceId);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.key, this.value, this.resourceId, this.userId);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .addValue(this.key)
      .addValue(this.value)
      .addValue(this.resourceId)
      .addValue(this.userId)
      .toString();
  }
}
