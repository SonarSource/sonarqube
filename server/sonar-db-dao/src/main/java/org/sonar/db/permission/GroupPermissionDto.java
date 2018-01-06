/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

  private String organizationUuid;
  private Integer groupId;
  private Long resourceId;
  private String role;

  public Integer getGroupId() {
    return groupId;
  }

  public String getOrganizationUuid() {
    return organizationUuid;
  }

  public GroupPermissionDto setOrganizationUuid(String s) {
    this.organizationUuid = s;
    return this;
  }

  /**
   * Null when Anyone
   */
  public GroupPermissionDto setGroupId(@Nullable Integer groupId) {
    this.groupId = groupId;
    return this;
  }

  @Nullable
  public Long getResourceId() {
    return resourceId;
  }

  public GroupPermissionDto setResourceId(@Nullable Long resourceId) {
    this.resourceId = resourceId;
    return this;
  }

  public String getRole() {
    return role;
  }

  public GroupPermissionDto setRole(String role) {
    this.role = role;
    return this;
  }
}
