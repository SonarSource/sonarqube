/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonar.db.audit.model;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.db.permission.GroupPermissionDto;
import org.sonar.db.permission.UserPermissionDto;

public class PermissionNewValue implements NewValue {
  @Nullable
  private String permissionUuid;

  @Nullable
  private String groupUuid;

  @Nullable
  private String userUuid;

  @Nullable
  private String projectUuid;

  @Nullable
  private String projectName;

  @Nullable
  private String role;

  public PermissionNewValue(GroupPermissionDto groupPermissionDto, String projectName) {
    this.permissionUuid = groupPermissionDto.getUuid();
    this.groupUuid = groupPermissionDto.getGroupUuid();
    this.projectUuid = groupPermissionDto.getComponentUuid();
    this.role = groupPermissionDto.getRole();
    this.projectName = projectName;
  }

  public PermissionNewValue(UserPermissionDto permissionDto, @Nullable String projectName) {
    this.permissionUuid = permissionDto.getUuid();
    this.userUuid = permissionDto.getUserUuid();
    this.projectUuid = permissionDto.getComponentUuid();
    this.role = permissionDto.getPermission();
    this.projectName = projectName;
  }

  public  PermissionNewValue(@Nullable String role, @Nullable String groupUuid, @Nullable String rootComponentUuid,
    @Nullable String projectName, @Nullable String userUuid) {
    this.role = role;
    this.groupUuid = groupUuid;
    this.projectUuid = rootComponentUuid;
    this.projectName = projectName;
    this.userUuid = userUuid;
  }

  @CheckForNull
  public String getPermissionUuid() {
    return this.permissionUuid;
  }

  @CheckForNull
  public String getGroupUuid() {
    return this.groupUuid;
  }

  @CheckForNull
  public String getProjectUuid() {
    return this.projectUuid;
  }

  @CheckForNull
  public String getRole() {
    return this.role;
  }

  @CheckForNull
  public String getProjectName() {
    return this.projectName;
  }

  @CheckForNull
  public String getUserUuid() {
    return this.userUuid;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("{");
    addField(sb, "\"permissionUuid\": ", this.permissionUuid, true);
    addField(sb, "\"groupUuid\": ", this.groupUuid, true);
    addField(sb, "\"projectUuid\": ", this.projectUuid, true);
    addField(sb, "\"role\": ", this.role, true);
    addField(sb, "\"projectName\": ", this.projectName, true);
    addField(sb, "\"userUuid\": ", this.userUuid, true);
    endString(sb);
    return sb.toString();
  }
}
