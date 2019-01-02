/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.db.property;

import com.google.common.base.MoreObjects;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;

public class PropertyDto {
  private static final int MAX_KEY_LENGTH = 512;

  private String key;
  private String value;
  private Long resourceId;
  private Integer userId;

  public String getKey() {
    return key;
  }

  public PropertyDto setKey(String key) {
    checkArgument(key.length() <= MAX_KEY_LENGTH, "Setting key length (%s) is longer than the maximum authorized (%s). '%s' was provided", key.length(), MAX_KEY_LENGTH, key);
    this.key = key;
    return this;
  }

  public String getValue() {
    return value;
  }

  public PropertyDto setValue(@Nullable String value) {
    this.value = value;
    return this;
  }

  @CheckForNull
  public Long getResourceId() {
    return resourceId;
  }

  public PropertyDto setResourceId(@Nullable Long resourceId) {
    this.resourceId = resourceId;
    return this;
  }

  @CheckForNull
  public Integer getUserId() {
    return userId;
  }

  public PropertyDto setUserId(@Nullable Integer userId) {
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
    PropertyDto other = (PropertyDto) obj;
    return Objects.equals(this.key, other.key)
      && Objects.equals(this.userId, other.userId)
      && Objects.equals(this.resourceId, other.resourceId)
      && Objects.equals(this.value, other.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.key, this.value, this.resourceId, this.userId);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .addValue(this.key)
      .addValue(this.value)
      .addValue(this.resourceId)
      .addValue(this.userId)
      .toString();
  }
}
