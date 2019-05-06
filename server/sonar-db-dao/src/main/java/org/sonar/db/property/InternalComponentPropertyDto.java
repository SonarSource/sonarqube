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
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;

public class InternalComponentPropertyDto {
  private static final int MAX_KEY_LENGTH = 512;
  private static final int MAX_VALUE_LENGTH = 4000;

  private String uuid;
  private String key;
  private String value;
  private String componentUuid;
  private Long createdAt;
  private Long updatedAt;

  public String getUuid() {
    return uuid;
  }

  public InternalComponentPropertyDto setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  public String getKey() {
    return key;
  }

  public InternalComponentPropertyDto setKey(String key) {
    checkArgument(key != null && !key.isEmpty(), "key can't be null nor empty");
    checkArgument(key.length() <= MAX_KEY_LENGTH, "key length (%s) is longer than the maximum authorized (%s). '%s' was provided", key.length(), MAX_KEY_LENGTH, key);
    this.key = key;
    return this;
  }

  public String getValue() {
    return value;
  }

  public InternalComponentPropertyDto setValue(@Nullable String value) {
    if (value != null) {
      checkArgument(value.length() <= MAX_VALUE_LENGTH, "value length (%s) is longer than the maximum authorized (%s). '%s' was provided", value.length(), MAX_VALUE_LENGTH, value);
    }
    this.value = value;
    return this;
  }

  public String getComponentUuid() {
    return componentUuid;
  }

  public InternalComponentPropertyDto setComponentUuid(String componentUuid) {
    this.componentUuid = componentUuid;
    return this;
  }

  public Long getCreatedAt() {
    return createdAt;
  }

  public InternalComponentPropertyDto setCreatedAt(Long createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Long getUpdatedAt() {
    return updatedAt;
  }

  public InternalComponentPropertyDto setUpdatedAt(Long updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper("InternalComponentPropertyDto")
      .add("uuid", this.uuid)
      .add("key", this.key)
      .add("value", this.value)
      .add("componentUuid", this.componentUuid)
      .add("updatedAt", this.updatedAt)
      .add("createdAt", this.createdAt)
      .toString();
  }
}
