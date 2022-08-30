/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.db.user;

import javax.annotation.Nullable;

public class UserTelemetryDto {

  private String uuid;
  private boolean active = true;
  @Nullable
  private Long lastConnectionDate;
  @Nullable
  private Long lastSonarlintConnectionDate;

  public String getUuid() {
    return uuid;
  }

  public UserTelemetryDto setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  public boolean isActive() {
    return active;
  }

  public UserTelemetryDto setActive(boolean active) {
    this.active = active;
    return this;
  }

  @Nullable
  public Long getLastConnectionDate() {
    return lastConnectionDate;
  }

  public UserTelemetryDto setLastConnectionDate(@Nullable Long lastConnectionDate) {
    this.lastConnectionDate = lastConnectionDate;
    return this;
  }

  @Nullable
  public Long getLastSonarlintConnectionDate() {
    return lastSonarlintConnectionDate;
  }

  public UserTelemetryDto setLastSonarlintConnectionDate(@Nullable Long lastSonarlintConnectionDate) {
    this.lastSonarlintConnectionDate = lastSonarlintConnectionDate;
    return this;
  }
}
