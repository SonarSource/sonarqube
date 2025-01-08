/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import org.sonar.db.permission.template.PermissionTemplateDto;

public class GroupPermissionNewValue extends PermissionNewValue {

  /**
   * @deprecated The uuids in the audit logs are not product requirement anymore and will be removed in 11.x
   */
  @Deprecated(since = "10.2")
  @Nullable
  private String groupUuid;

  @Nullable
  private String groupName;

  public GroupPermissionNewValue(String uuid, String componentUuid,  String componentKey, String componentName, String role, String groupUuid, String groupName,
    String qualifier, @Nullable PermissionTemplateDto permissionTemplate) {
    super(uuid, componentUuid, componentKey, componentName, role, qualifier, permissionTemplate);
    this.groupUuid = groupUuid;
    this.groupName = groupName;
  }
  public GroupPermissionNewValue(String componentUuid, String componentKey, String componentName, String role, String groupUuid, String groupName, String qualifier) {
    this(null, componentUuid, componentKey, componentName, role, groupUuid, groupName, qualifier, null);
  }

  public GroupPermissionNewValue(GroupPermissionDto dto, @Nullable String componentKey, @Nullable String qualifier, @Nullable PermissionTemplateDto permissionTemplate) {
    this(dto.getUuid(), dto.getEntityUuid(), componentKey, dto.getEntityName(), dto.getRole(), dto.getGroupUuid(), dto.getGroupName(), qualifier, permissionTemplate);
  }

  /**
   * @deprecated The uuids in the audit logs are not product requirement anymore and will be removed in 11.x
   */
  @Deprecated(since = "10.2")
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
    addField(sb, "\"componentKey\": ", this.componentKey, true);
    addField(sb, "\"componentName\": ", this.componentName, true);
    addField(sb, "\"permissionTemplateUuid\": ", this.permissionTemplateId, true);
    addField(sb, "\"permissionTemplateName\": ", this.permissionTemplateName, true);
    addField(sb, "\"qualifier\": ", getQualifier(this.qualifier), true);
    endString(sb);
    return sb.toString();
  }

}
