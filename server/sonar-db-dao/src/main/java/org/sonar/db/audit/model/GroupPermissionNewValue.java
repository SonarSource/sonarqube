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

import javax.annotation.Nullable;
import org.sonar.db.permission.GroupPermissionDto;

public class GroupPermissionNewValue extends PermissionNewValue {

  @Nullable
  private String groupUuid;

  @Nullable
  private String groupName;

  public GroupPermissionNewValue(String uuid, String rootComponentUuid, String componentName, String role, String groupUuid,
    String groupName, String qualifier) {
    super(uuid, rootComponentUuid, componentName, role, qualifier);
    this.groupUuid = groupUuid;
    this.groupName = groupName;
  }

  public GroupPermissionNewValue(String rootComponentUuid, String componentName, String role, String groupUuid,
    String groupName, String qualifier) {
    this(null, rootComponentUuid, componentName, role, groupUuid, groupName, qualifier);
  }

  public GroupPermissionNewValue(GroupPermissionDto dto, String qualifier) {
    this(dto.getUuid(), dto.getComponentUuid(), dto.getComponentName(), dto.getRole(), dto.getGroupUuid(),
      dto.getGroupName(), qualifier);
  }

  @Nullable
  public String getGroupUuid() {
    return groupUuid;
  }

  @Nullable
  public String getGroupName() {
    return groupName;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("{");
    addField(sb, "\"permissionUuid\": ", this.permissionUuid, true);
    addField(sb, "\"permission\": ", this.permission, true);
    addField(sb, "\"groupUuid\": ", this.groupUuid, true);
    addField(sb, "\"groupName\": ", this.groupName, true);
    addField(sb, "\"componentUuid\": ", this.componentUuid, true);
    addField(sb, "\"componentName\": ", this.componentName, true);
    addField(sb, "\"qualifier\": ", this.qualifier, true);
    endString(sb);
    return sb.toString();
  }

}
