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

public class PermissionNewValue extends NewValue {
  @Nullable
  private String permissionUuid;

  @Nullable
  private String groupUuid;

  @Nullable
  private String groupName;

  @Nullable
  private String userUuid;

  @Nullable
  private String componentUuid;

  @Nullable
  private String componentName;

  @Nullable
  private String role;

  @Nullable
  private String qualifier;

  public PermissionNewValue(GroupPermissionDto groupPermissionDto, @Nullable String componentName, @Nullable String qualifier) {
    this.permissionUuid = groupPermissionDto.getUuid();
    this.role = groupPermissionDto.getRole();
    this.groupUuid = groupPermissionDto.getGroupUuid();
    this.groupName = groupPermissionDto.getGroupName();
    this.componentUuid = groupPermissionDto.getComponentUuid();
    this.role = groupPermissionDto.getRole();
    this.componentName = componentName;
    this.qualifier = getQualifier(qualifier);
  }

  public PermissionNewValue(UserPermissionDto permissionDto, @Nullable String componentName, @Nullable String qualifier) {
    this.permissionUuid = permissionDto.getUuid();
    this.userUuid = permissionDto.getUserUuid();
    this.componentUuid = permissionDto.getComponentUuid();
    this.role = permissionDto.getPermission();
    this.componentName = componentName;
    this.qualifier = getQualifier(qualifier);
  }

  public  PermissionNewValue(@Nullable String role, @Nullable String groupUuid, @Nullable String groupName, @Nullable String rootComponentUuid,
    @Nullable String componentName, @Nullable String qualifier, @Nullable String userUuid) {
    this.role = role;
    this.groupUuid = groupUuid;
    this.groupName = groupName;
    this.componentUuid = rootComponentUuid;
    this.componentName = componentName;
    this.userUuid = userUuid;
    this.qualifier = getQualifier(qualifier);
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
  public String getGroupName() {
    return this.groupName;
  }

  @CheckForNull
  public String getComponentUuid() {
    return this.componentUuid;
  }

  @CheckForNull
  public String getRole() {
    return this.role;
  }

  @CheckForNull
  public String getComponentName() {
    return this.componentName;
  }

  @CheckForNull
  public String getQualifier() {
    return this.qualifier;
  }

  @CheckForNull
  public String getUserUuid() {
    return this.userUuid;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("{");
    addField(sb, "\"permissionUuid\": ", this.permissionUuid, true);
    addField(sb, "\"role\": ", this.role, true);
    addField(sb, "\"groupUuid\": ", this.groupUuid, true);
    addField(sb, "\"groupName\": ", this.groupName, true);
    addField(sb, "\"componentUuid\": ", this.componentUuid, true);
    addField(sb, "\"componentName\": ", this.componentName, true);
    addField(sb, "\"qualifier\": ", this.qualifier, true);
    addField(sb, "\"userUuid\": ", this.userUuid, true);
    endString(sb);
    return sb.toString();
  }
}
