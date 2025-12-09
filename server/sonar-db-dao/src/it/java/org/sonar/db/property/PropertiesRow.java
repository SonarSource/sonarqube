/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

final class PropertiesRow {
  private final String key;
  private final String userUuid;
  private final String componentUuid;
  private final Boolean empty;
  private final String textValue;
  private final String clobValue;
  private final Long createdAt;

  public PropertiesRow(String key, @Nullable String userUuid, @Nullable String componentUuid,
    @Nullable Boolean empty, @Nullable String textValue, @Nullable String clobValue,
    @Nullable Long createdAt) {
    this.key = key;
    this.userUuid = userUuid;
    this.componentUuid = componentUuid;
    this.empty = empty;
    this.textValue = textValue;
    this.clobValue = clobValue;
    this.createdAt = createdAt;
  }

  public String getKey() {
    return key;
  }

  public String getUserUuid() {
    return userUuid;
  }

  public String getComponentUuid() {
    return componentUuid;
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
