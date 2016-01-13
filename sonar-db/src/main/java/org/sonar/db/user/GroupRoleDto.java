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

import javax.annotation.Nullable;

/**
 * @since 3.2
 */
public class GroupRoleDto {
  private Long id;
  private Long groupId;
  private Long resourceId;
  private String role;

  public Long getId() {
    return id;
  }

  public GroupRoleDto setId(Long id) {
    this.id = id;
    return this;
  }

  public Long getGroupId() {
    return groupId;
  }

  /**
   * Null when Anyone
   */
  public GroupRoleDto setGroupId(@Nullable Long groupId) {
    this.groupId = groupId;
    return this;
  }

  @Nullable
  public Long getResourceId() {
    return resourceId;
  }

  public GroupRoleDto setResourceId(@Nullable Long resourceId) {
    this.resourceId = resourceId;
    return this;
  }

  public String getRole() {
    return role;
  }

  public GroupRoleDto setRole(String role) {
    this.role = role;
    return this;
  }
}
