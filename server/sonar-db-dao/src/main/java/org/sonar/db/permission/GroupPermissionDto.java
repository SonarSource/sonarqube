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
package org.sonar.db.permission;

import javax.annotation.Nullable;

public class GroupPermissionDto {
  private String uuid;
  private String role;
  private String groupUuid;
  private String groupName;
  private String componentUuid;
  private String componentName;

  public String getUuid() {
    return uuid;
  }

  public GroupPermissionDto setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  public String getGroupUuid() {
    return groupUuid;
  }

  /**
   * Null when Anyone
   */
  public GroupPermissionDto setGroupUuid(@Nullable String groupUuid) {
    this.groupUuid = groupUuid;
    return this;
  }

  @Nullable
  public String getComponentUuid() {
    return componentUuid;
  }

  public GroupPermissionDto setComponentUuid(@Nullable String componentUuid) {
    this.componentUuid = componentUuid;
    return this;
  }

  public String getRole() {
    return role;
  }

  public GroupPermissionDto setRole(String role) {
    this.role = role;
    return this;
  }

  @Nullable
  public String getGroupName() {
    return groupName;
  }

  public GroupPermissionDto setGroupName(@Nullable String groupName) {
    this.groupName = groupName;
    return this;
  }

  @Nullable
  public String getComponentName() {
    return componentName;
  }

  public GroupPermissionDto setComponentName(@Nullable String componentName) {
    this.componentName = componentName;
    return this;
  }
}
