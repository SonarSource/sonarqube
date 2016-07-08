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
package org.sonar.db.user;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class UserPermissionDto {
  private Long userId;
  private Long componentId;
  private String permission;

  public Long getUserId() {
    return userId;
  }

  public UserPermissionDto setUserId(Long userId) {
    this.userId = userId;
    return this;
  }

  @CheckForNull
  public Long getComponentId() {
    return componentId;
  }

  public UserPermissionDto setComponentId(@Nullable Long componentId) {
    this.componentId = componentId;
    return this;
  }

  public String getPermission() {
    return permission;
  }

  public UserPermissionDto setPermission(String permission) {
    this.permission = permission;
    return this;
  }
}
