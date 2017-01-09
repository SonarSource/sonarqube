/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

final class PropertiesRow {
  private final String key;
  private final Long userId;
  private final Long resourceId;
  private final Boolean empty;
  private final String textValue;
  private final String clobValue;
  private final Long createdAt;

  public PropertiesRow(String key, @Nullable Long userId, @Nullable Long resourceId,
    @Nullable Boolean empty, @Nullable String textValue, @Nullable String clobValue,
    @Nullable Long createdAt) {
    this.key = key;
    this.userId = userId;
    this.resourceId = resourceId;
    this.empty = empty;
    this.textValue = textValue;
    this.clobValue = clobValue;
    this.createdAt = createdAt;
  }

  public String getKey() {
    return key;
  }

  public Long getUserId() {
    return userId;
  }

  public Long getResourceId() {
    return resourceId;
  }

  @CheckForNull
  public Boolean getEmpty() {
    return empty;
  }

  @CheckForNull
  public String getTextValue() {
    return textValue;
  }

  @CheckForNull
  public String getClobValue() {
    return clobValue;
  }

  @CheckForNull
  public Long getCreatedAt() {
    return createdAt;
  }
}
