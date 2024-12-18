/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.db.user.ai;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class UserAiToolUsageDto {

  private String uuid = null;
  private String userUuid = null;
  private Long activatedAt = null;
  private Long lastActivityAt = null;

  public String getUuid() {
    return uuid;
  }

  public UserAiToolUsageDto setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  public String getUserUuid() {
    return userUuid;
  }

  public UserAiToolUsageDto setUserUuid(String userUuid) {
    this.userUuid = userUuid;
    return this;
  }

  public Long getActivatedAt() {
    return activatedAt;
  }

  public UserAiToolUsageDto setActivatedAt(Long activatedAt) {
    this.activatedAt = activatedAt;
    return this;
  }

  @CheckForNull
  public Long getLastActivityAt() {
    return lastActivityAt;
  }

  public UserAiToolUsageDto setLastActivityAt(@Nullable Long lastActivityAt) {
    this.lastActivityAt = lastActivityAt;
    return this;
  }
}
